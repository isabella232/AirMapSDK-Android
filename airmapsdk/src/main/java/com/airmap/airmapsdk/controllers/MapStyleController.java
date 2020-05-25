package com.airmap.airmapsdk.controllers;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.provider.Settings;
import android.text.TextUtils;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.TemporalFilter;
import com.airmap.airmapsdk.models.map.AirMapLayerStyle;
import com.airmap.airmapsdk.models.map.MapStyle;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.networking.services.MappingService;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.AirMapConfig;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.BackgroundLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

import static android.graphics.Color.TRANSPARENT;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Dark;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Light;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Satellite;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Standard;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillPattern;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapStyleController implements MapView.OnDidFinishLoadingStyleListener {

    private AirMapMapView map;

    private MappingService.AirMapMapTheme currentTheme;
    private MapStyle mapStyle;
    private Callback callback;

    private String highlightLayerId;

    private static String tileJsonSpecVersion = "2.2.0";
    private TemporalFilter temporalFilter = null;

    // If jurisdictionAllowed is null, it means no whitelist (allow all)
    private List<Integer> jurisdictionAllowed = null;

    public MapStyleController(AirMapMapView map, @Nullable MappingService.AirMapMapTheme mapTheme, Callback callback) {
        this.map = map;
        this.callback = callback;

        if (mapTheme != null) {
            currentTheme = mapTheme;
        } else {
            // use last used theme or Standard if none has be saved
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(map.getContext());
            String savedTheme = prefs.getString(AirMapConstants.MAP_STYLE, MappingService.AirMapMapTheme.Standard.toString());
            currentTheme = MappingService.AirMapMapTheme.fromString(savedTheme);
        }

        JSONArray whitelist = AirMapConfig.getMapAllowedJurisdictions();
        if(whitelist != null){
            jurisdictionAllowed = new ArrayList<>();
            for (int i = 0; i < whitelist.length(); i++) {
                jurisdictionAllowed.add(whitelist.optInt(i));
            }
        }

        // On the receipt of a new Auth Token, reload the current style to populate Enterprise
        // AirMap.setAuthTokenListener(() -> map.post(this::setupJurisdictionsForEnterprise));
    }

    public void onMapReady() {
        loadStyleJSON();

        map.addOnDidFinishLoadingStyleListener(this);
    }

    @Override
    public void onDidFinishLoadingStyle() {
        // Adjust the background overlay opacity to improve map visually on Android
        BackgroundLayer backgroundLayer = map.getMap().getStyle().getLayerAs("background-overlay");
        if (backgroundLayer != null) {
            if (currentTheme == MappingService.AirMapMapTheme.Light || currentTheme == MappingService.AirMapMapTheme.Standard) {
                backgroundLayer.setProperties(PropertyFactory.backgroundOpacity(0.95f));
            } else if (currentTheme == MappingService.AirMapMapTheme.Dark) {
                backgroundLayer.setProperties(PropertyFactory.backgroundOpacity(0.9f));
            }
        }

        try {
            mapStyle = new MapStyle(map.getMap().getStyle().getJson());
        } catch (JSONException e) {
            Timber.e(e, "Failed to parse style json");
        }

        // change labels to local if device is not in english
        if (!Locale.ENGLISH.getLanguage().equals(Locale.getDefault().getLanguage())) {
            for (Layer layer : map.getMap().getStyle().getLayers()) {
                if (layer instanceof SymbolLayer && (layer.getId().contains("label") || layer.getId().contains("place") || layer.getId().contains("poi"))) {
                    //layer.setProperties(PropertyFactory.textField("{name}"));
                    // TODO: 2020-01-15 Need to do more investigation as to why removing this line fixes map labelling issue. 
                }
            }
        }

        callback.onMapStyleLoaded();
    }

    private void setupJurisdictionsForEnterprise() {
        // Reload the style after setup is complete
        if (map == null || map.getMap() == null || map.getMap().getStyle() == null) {
            return;
        }

        OfflineManager.getInstance(map.getContext()).clearAmbientCache(null);

        Style style = map.getMap().getStyle();
        String jurisdictionsId = "jurisdictions";

        if (style.getLayer(jurisdictionsId) != null) {
            style.removeLayer(jurisdictionsId);
        }

        if (style.getSource(jurisdictionsId) != null) {
            style.removeSource(jurisdictionsId);
        }

        TileSet tileSet = new TileSet(tileJsonSpecVersion, AirMap.getBaseJurisdictionsUrlTemplate());
        tileSet.setMaxZoom(12f);
        tileSet.setMinZoom(8f);
        Source source = new VectorSource(jurisdictionsId, tileSet);
        style.addSource(source);
        Layer layer = new FillLayer(jurisdictionsId, jurisdictionsId)
                .withSourceLayer(jurisdictionsId)
                .withProperties(fillColor(TRANSPARENT), fillOpacity(1f));
        style.addLayerAt(layer, 0);
    }

    // Updates the map to use a custom style based on theme and selected layers
    public void rotateMapTheme() {
        MappingService.AirMapMapTheme theme = Standard;
        switch (currentTheme) {
            case Standard: {
                Analytics.logEvent(Analytics.Event.map, Analytics.Action.select, "Dark");
                theme = Dark;
                break;
            }
            case Dark: {
                Analytics.logEvent(Analytics.Event.map, Analytics.Action.select, "Light");
                theme = Light;
                break;
            }
            case Light: {
                Analytics.logEvent(Analytics.Event.map, Analytics.Action.select, "Satellite");
                theme = Satellite;
                break;
            }
            case Satellite: {
                Analytics.logEvent(Analytics.Event.map, Analytics.Action.select, "Standard");
                theme = Standard;
                break;
            }
        }

        updateMapTheme(theme);
    }

    public void reset() {
        callback.onMapStyleReset();
        loadStyleJSON();
    }

    public void updateMapTheme(MappingService.AirMapMapTheme theme) {
        callback.onMapStyleReset();

        currentTheme = theme;
        loadStyleJSON();

        PreferenceManager.getDefaultSharedPreferences(map.getContext()).edit().putString(AirMapConstants.MAP_STYLE, currentTheme.toString()).apply();
    }

    public void hideInactiveAirspace(){
        Expression hasActive = Expression.has("active");
        Expression activeIsTrue = Expression.eq(Expression.get("active"), true);
        Expression active = Expression.all(hasActive, activeIsTrue);
        map.getMap().getStyle(style -> {
            for(Layer layer : Objects.requireNonNull(style.getLayers())){
                if(layer.getId().contains("airmap")){
                    if(layer instanceof FillLayer){
                        if(((FillLayer) layer).getFilter() != null){
                            ((FillLayer) layer).setFilter(Expression.any(((FillLayer) layer).getFilter(), active));
                        } else {
                            ((FillLayer) layer).setFilter(active);
                        }

                    } else if (layer instanceof  LineLayer){
                        if(((LineLayer) layer).getFilter() != null){
                            ((LineLayer) layer).setFilter(Expression.any(((LineLayer) layer).getFilter(), active));
                        } else {
                            ((LineLayer) layer).setFilter(active);
                        }

                    } else if(layer instanceof  SymbolLayer){
                        if(((SymbolLayer) layer).getFilter() != null){
                            ((SymbolLayer) layer).setFilter(Expression.any(((SymbolLayer) layer).getFilter(), active));
                        } else {
                            ((SymbolLayer) layer).setFilter(active);
                        }

                    } else {
                        Timber.e("Unknown layer");

                    }
                }
            }
        });
    }

    public void addMapLayers(String sourceId, List<String> layers, boolean useSIMeasurements) {
        // check if source is already added to map
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        if(temporalFilter != null){
            switch (temporalFilter.getType()){
                case NOW:
                    switch (temporalFilter.getRange()){

                        case ONE_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 1);
                            break;
                        case FOUR_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 4);
                            break;
                        case EIGHT_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 8);
                            break;
                        case TWELVE_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 12);
                            break;
                    }
                    break;
                case CUSTOM:
                    cal1.setTime(temporalFilter.getFutureDate());
                    cal2.setTime(temporalFilter.getFutureDate());

                    cal1.set(Calendar.HOUR_OF_DAY, temporalFilter.getStartHour());
                    cal1.set(Calendar.MINUTE, temporalFilter.getStartMinute());

                    cal2.set(Calendar.HOUR_OF_DAY, temporalFilter.getEndHour());
                    cal2.set(Calendar.MINUTE, temporalFilter.getEndMinute());
                    break;
            }
        }

        if (map.getMap().getStyle().getSource(sourceId) != null) {
            Timber.e("Source already added for: %s", sourceId);
        } else {
            String urlTemplates;
            if(temporalFilter == null){
                urlTemplates = AirMap.getRulesetTileUrlTemplate(sourceId, layers, useSIMeasurements);
            } else {
                urlTemplates = AirMap.getRulesetTileUrlTemplate(sourceId, layers, useSIMeasurements, cal1.getTime(), cal2.getTime());
            }
            TileSet tileSet = new TileSet(tileJsonSpecVersion, urlTemplates);
            tileSet.setMaxZoom(12f);
            tileSet.setMinZoom(8f);
            VectorSource tileSource = new VectorSource(sourceId, tileSet);
            map.getMap().getStyle().addSource(tileSource);
        }

        for (String sourceLayer : layers) {
            for (AirMapLayerStyle layerStyle : mapStyle.getLayerStyles(sourceLayer)) {
                // check if layer is already added to map
                if (map.getMap().getStyle().getLayer(layerStyle.id + "|" + sourceId + "|new") != null) {
                    continue;
                }

                // use layer from styles as a template
                Layer layerToClone = map.getMap().getStyle().getLayerAs(layerStyle.id);

                Layer layer = layerStyle.toMapboxLayer(layerToClone, sourceId);

                // add temporal filter if applicable
                if (layer.getId().contains("airmap|tfr") || layer.getId().contains("notam")) {
                    addTemporalFilter(layer);
                }

                map.getMap().getStyle().addLayerAbove(layer, layerStyle.id);
            }
        }

        // add highlight layer
        if (map.getMap().getStyle().getLayer("airmap|highlight|line|" + sourceId) == null) {
            LineLayer highlightLayer = new LineLayer("airmap|highlight|line|" + sourceId, sourceId);
            highlightLayer.setProperties(PropertyFactory.lineColor("#f9e547"));
            highlightLayer.setProperties(PropertyFactory.lineWidth(4f));
            highlightLayer.setProperties(PropertyFactory.lineOpacity(0.9f));
            Expression filter = Expression.eq(Expression.get("id"), "x");

            try {
                highlightLayer.setFilter(filter);
                map.getMap().getStyle().addLayer(highlightLayer);
            } catch (Throwable t) {
                // https://github.com/mapbox/mapbox-gl-native/issues/10947
                // https://github.com/mapbox/mapbox-gl-native/issues/11264
                // A layer is associated with a style, not the mapView/mapbox
                Analytics.report(new Exception(t));
                t.printStackTrace();
            }
        }
    }

    private void addTemporalFilter(Layer layer) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        long start = System.currentTimeMillis() / 1000;
        long end = start + (4 * 60 * 60);

        if(temporalFilter != null){
            switch (temporalFilter.getType()){
                case NOW:
                    switch (temporalFilter.getRange()){

                        case ONE_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 1);
                            break;
                        case FOUR_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 4);
                            break;
                        case EIGHT_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 8);
                            break;
                        case TWELVE_HOUR:
                            cal2.roll(Calendar.HOUR_OF_DAY, 12);
                            break;
                    }
                    break;
                case CUSTOM:
                    cal1.setTime(temporalFilter.getFutureDate());
                    cal2.setTime(temporalFilter.getFutureDate());

                    cal1.set(Calendar.HOUR_OF_DAY, temporalFilter.getStartHour());
                    cal1.set(Calendar.MINUTE, temporalFilter.getStartMinute());

                    cal2.set(Calendar.HOUR_OF_DAY, temporalFilter.getEndHour());
                    cal2.set(Calendar.MINUTE, temporalFilter.getEndMinute());
                    break;
            }

            start = cal1.getTime().getTime() / 1000;
            end = cal2.getTime().getTime() / 1000;
        }

        Expression validNowFilter = Expression.all(Expression.lt(Expression.get("start"), start), Expression.gt(Expression.get("end"), start));
        Expression startsSoonFilter = Expression.all(Expression.gt(Expression.get("start"), start), Expression.lt(Expression.get("start"), end));
        Expression permanent = Expression.all(Expression.has("permanent"), Expression.eq(Expression.get("permanent"), "true"));
        Expression hasNoEnd = Expression.all(Expression.not(Expression.has("end")), Expression.not(Expression.has("base")));
        Expression filter = Expression.any(permanent, hasNoEnd, validNowFilter, startsSoonFilter);

        if (layer instanceof FillLayer) {
            ((FillLayer) layer).setFilter(filter);
        } else if (layer instanceof LineLayer) {
            ((LineLayer) layer).setFilter(filter);
        }
    }

    public void removeMapLayers(String sourceId, List<String> sourceLayers) {
        if (sourceLayers == null || sourceLayers.isEmpty()) {
            return;
        }

        Timber.v("remove source: %s layers: %s", sourceId, TextUtils.join(",", sourceLayers));

        for (String sourceLayer : sourceLayers) {
            for (AirMapLayerStyle layerStyle : mapStyle.getLayerStyles(sourceLayer)) {
                map.getMap().getStyle().removeLayer(layerStyle.id + "|" + sourceId + "|new");
            }
        }

        // remove highlight
        map.getMap().getStyle().removeLayer("airmap|highlight|line|" + sourceId);
        if (highlightLayerId != null && highlightLayerId.equals("airmap|highlight|line|" + sourceId)) {
            highlightLayerId = null;
        }

        map.getMap().getStyle().removeSource(sourceId);
    }

    public void highlight(@NonNull Feature feature, AirMapAdvisory advisory) {
        // remove old highlight
        unhighlight();

        // add new highlight
        String sourceId = feature.getStringProperty("ruleset_id");
        highlightLayerId = "airmap|highlight|line|" + sourceId;
        LineLayer highlightLayer = map.getMap().getStyle().getLayerAs(highlightLayerId);
        highlightLayer.setSourceLayer(sourceId + "_" + advisory.getType().toString());

        // feature's airspace_id can be an int or string (tile server bug), so match on either
        Expression filter;
        try {
            int airspaceId = Integer.parseInt(advisory.getId());
            filter = Expression.any(Expression.eq(Expression.get("id"), advisory.getId()), Expression.eq(Expression.get("id"), airspaceId));
        } catch (NumberFormatException e) {
            filter = Expression.any(Expression.eq(Expression.get("id"), advisory.getId()));
        }
        highlightLayer.setFilter(filter);
    }

    public void highlight(Feature feature) {
        String id = feature.getStringProperty("id");
        String type = feature.getStringProperty("category");

        // remove old highlight
        unhighlight();

        // add new highlight
        String sourceId = feature.getStringProperty("ruleset_id");
        highlightLayerId = "airmap|highlight|line|" + sourceId;
        LineLayer highlightLayer = map.getMap().getStyle().getLayerAs(highlightLayerId);
        highlightLayer.setSourceLayer(sourceId + "_" + type);

        // feature's airspace_id can be an int or string (tile server bug), so match on either
        Expression filter;
        try {
            int airspaceId = Integer.parseInt(id);
            filter = Expression.any(Expression.eq(Expression.get("id"), id), Expression.eq(Expression.get("id"), airspaceId));
        } catch (NumberFormatException e) {
            filter = Expression.any(Expression.eq(Expression.get("id"), id));
        }
        highlightLayer.setFilter(filter);
    }

    public void unhighlight() {
        if (highlightLayerId != null) {
            try {
                LineLayer oldHighlightLayer = map.getMap().getStyle().getLayerAs(highlightLayerId);
                if (oldHighlightLayer != null) {
                    Expression filter = Expression.eq(Expression.get("id"), "x");
                    oldHighlightLayer.setFilter(filter);
                }
            } catch (RuntimeException e) {
                for (Layer l : map.getMap().getStyle().getLayers()) {
                    if (l instanceof LineLayer) {
                        Expression filter = Expression.eq(Expression.get("id"), "x");
                        ((LineLayer) l).setFilter(filter);
                    }
                }
                Analytics.report(e);
            }
        }
    }

    public void setTemporalFilter(TemporalFilter temporalFilter){
        this.temporalFilter = temporalFilter;
        reset();
    }

    private void loadStyleJSON() {
        map.getMap().setStyle(AirMap.getMapStylesUrl(currentTheme), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                String jurisdictionsId = "jurisdictions";

                if (style.getLayer(jurisdictionsId) != null) {
                    style.removeLayer(jurisdictionsId);
                }

                if (style.getSource(jurisdictionsId) != null) {
                    style.removeSource(jurisdictionsId);
                }

                TileSet tileSet = new TileSet(tileJsonSpecVersion, AirMap.getBaseJurisdictionsUrlTemplate());
                tileSet.setMaxZoom(12f);
                tileSet.setMinZoom(8f);
                Source source = new VectorSource(jurisdictionsId, tileSet);
                style.addSource(source);
                Layer layer = new FillLayer(jurisdictionsId, jurisdictionsId)
                        .withSourceLayer(jurisdictionsId)
                        .withProperties(fillColor(TRANSPARENT), fillOpacity(1f));
                style.addLayerAt(layer, 0);

                if(jurisdictionAllowed != null){

                    Integer[] jurisdictionsIdArray = new Integer[jurisdictionAllowed.size()];
                    jurisdictionsIdArray = jurisdictionAllowed.toArray(jurisdictionsIdArray);

                    String disabledLayerPrefix = "airmap|disabled_jurisdictions|";
                    Expression isFederalFilter = Expression.eq(Expression.get("region"), "federal");
                    Expression jurisdictionArray = Expression.literal(jurisdictionsIdArray);
                    Expression idInJurisdictionArray = Expression.in(Expression.get("id"), jurisdictionArray);
                    Expression notIdInJurisdictionArray = Expression.not(idInJurisdictionArray);
                    Expression filter1 = Expression.all(isFederalFilter, notIdInJurisdictionArray);

                    BackgroundLayer background = (BackgroundLayer) style.getLayer("background");

                    assert background != null;
                    FillLayer boundsFill1 = new FillLayer(disabledLayerPrefix + "fill|0", source.getId())
                            .withSourceLayer(source.getId())
                            .withProperties(fillColor(background.getBackgroundColor().getExpression()), fillOpacity(0.8f))
                            .withFilter(filter1);

                    style.addLayer(boundsFill1);

                    FillLayer boundsFill2 = new FillLayer(disabledLayerPrefix + "fill|1", source.getId())
                            .withSourceLayer(source.getId())
                            .withProperties(fillPattern("heliports_lines_pattern"))
                            .withFilter(filter1);

                    style.addLayer(boundsFill2);

                    LineLayer boundsLine = new LineLayer(disabledLayerPrefix + "line|0", source.getId())
                            .withSourceLayer(source.getId())
                            .withProperties(lineWidth((float) 2), lineColor(Color.DKGRAY))
                            .withFilter(idInJurisdictionArray);

                    style.addLayer(boundsLine);
                }
            }
        });
    }

    public void checkConnection(final AirMapCallback<Void> callback) {
        AirMap.getMapStylesJson(MappingService.AirMapMapTheme.Standard, new AirMapCallback<JSONObject>() {
            @Override
            protected void onSuccess(JSONObject response) {
                callback.success(null);
            }

            @Override
            protected void onError(AirMapException e) {
                callback.error(e);
            }
        });
    }

    public interface Callback {
        void onMapStyleLoaded();

        void onMapStyleReset();
    }
}
