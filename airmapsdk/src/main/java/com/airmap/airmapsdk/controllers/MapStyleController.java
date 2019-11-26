package com.airmap.airmapsdk.controllers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.models.map.AirMapLayerStyle;
import com.airmap.airmapsdk.models.map.MapStyle;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.networking.services.MappingService;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.BackgroundLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static android.graphics.Color.TRANSPARENT;
import static com.airmap.airmapsdk.networking.services.BaseService.mapTilesBaseJurisdictionsUrl;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Dark;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Light;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Satellite;
import static com.airmap.airmapsdk.networking.services.MappingService.AirMapMapTheme.Standard;
import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;

public class MapStyleController implements MapView.OnDidFinishLoadingStyleListener {

    private AirMapMapView map;

    private MappingService.AirMapMapTheme currentTheme;
    private MapStyle mapStyle;
    private Callback callback;

    private String highlightLayerId;

    private static String tileJsonSpecVersion = "2.2.0";

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

        AirMap.setAuthTokenListener(this::setupJurisdictionsForEnterprise);
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

        setupJurisdictionsForEnterprise();

        try {
            mapStyle = new MapStyle(map.getMap().getStyle().getJson());
        } catch (JSONException e) {
            Timber.e(e, "Failed to parse style json");
        }

        // change labels to local if device is not in english
        if (!Locale.ENGLISH.getLanguage().equals(Locale.getDefault().getLanguage())) {
            for (Layer layer : map.getMap().getStyle().getLayers()) {
                if (layer instanceof SymbolLayer && (layer.getId().contains("label") || layer.getId().contains("place") || layer.getId().contains("poi"))) {
                    layer.setProperties(PropertyFactory.textField("{name}"));
                }
            }
        }

        callback.onMapStyleLoaded();
    }

    private void setupJurisdictionsForEnterprise() {
        // Enterprise only applies when we have an access token
        if (TextUtils.isEmpty(AirMap.getAuthToken()) || map == null || map.getMap() == null || map.getMap().getStyle() == null) {
            return;
        }

        String jurisdictionsUrl = mapTilesBaseJurisdictionsUrl + "?accessToken=" + AirMap.getAuthToken();
        TileSet tileSet = new TileSet(tileJsonSpecVersion, jurisdictionsUrl);

        String jurisdictionsId = "jurisdictions";

        if (map.getMap().getStyle().getLayer(jurisdictionsId) != null) {
            map.getMap().getStyle().removeLayer(jurisdictionsId);
        }

        if (map.getMap().getStyle().getSource(jurisdictionsId) != null) {
            map.getMap().getStyle().removeSource(jurisdictionsId);
        }

        VectorSource vectorSource = new VectorSource(jurisdictionsId, tileSet);
        FillLayer fillLayer = new FillLayer(jurisdictionsId, jurisdictionsId)
                .withProperties(fillColor(color(TRANSPARENT)), fillOpacity(literal(1)));
        fillLayer.setSourceLayer(jurisdictionsId);


        map.getMap().getStyle().addSource(vectorSource);
        map.getMap().getStyle().addLayerAt(fillLayer, 0);
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

    public void addMapLayers(String sourceId, List<String> layers, boolean useSIMeasurements) {
        // check if source is already added to map
        if (map.getMap().getStyle().getSource(sourceId) != null) {
            Timber.e("Source already added for: %s", sourceId);
        } else {
            String urlTemplates = AirMap.getRulesetTileUrlTemplate(sourceId, layers, useSIMeasurements);
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
        long now = System.currentTimeMillis() / 1000;
        long in4Hrs = now + (4 * 60 * 60);
        Expression validNowFilter = Expression.all(Expression.lt(Expression.get("start"), now), Expression.gt(Expression.get("end"), now));
        Expression startsSoonFilter = Expression.all(Expression.gt(Expression.get("start"), now), Expression.lt(Expression.get("start"), in4Hrs));
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

    private void loadStyleJSON() {
        map.getMap().setStyle(AirMap.getMapStylesUrl(currentTheme));
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
