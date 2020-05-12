package com.airmap.airmapsdk.controllers;

import android.graphics.RectF;
import android.os.Handler;
import android.text.TextUtils;

import androidx.core.util.Pair;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.TemporalFilter;
import com.airmap.airmapsdk.models.rules.AirMapJurisdiction;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.CopyCollections;
import com.airmap.airmapsdk.util.RetryWithDelay;
import com.airmap.airmapsdk.util.ThrottleablePublishSubject;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.geometry.VisibleRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class MapDataController {

    protected ThrottleablePublishSubject<AirMapPolygon> jurisdictionsPublishSubject;
    protected PublishSubject<AirMapMapView.Configuration> configurationPublishSubject;
    private Subscription rulesetsSubscription;

    private AirMapMapView map;

    private boolean hasJurisdictions;
    private boolean fetchAdvisories;

    private List<AirMapRuleset> selectedRulesets;
    private List<AirMapRuleset> availableRulesets;
    private AirMapAirspaceStatus airspaceStatus;

    private Callback callback;
    private TemporalFilter temporalFilter = null;

    private AirMapMapView.Configuration configuration;

    // If jurisdictionAllowed is null, it means no whitelist (allow all)
    private List<Integer> jurisdictionAllowed = null;

    public MapDataController(AirMapMapView map, AirMapMapView.Configuration configuration) {
        this(map, configuration, new TemporalFilter(TemporalFilter.Range.FOUR_HOUR));
    }

    public MapDataController(AirMapMapView map, AirMapMapView.Configuration configuration, TemporalFilter temporalFilter){
        this.map = map;
        this.callback = map;

        jurisdictionsPublishSubject = ThrottleablePublishSubject.create();
        configurationPublishSubject = PublishSubject.create();
        this.temporalFilter = temporalFilter;
        this.configuration = configuration;
        fetchAdvisories = true;
        JSONObject config = AirMap.getConfig();
        if (config.has("app") && config.optJSONObject("app").has("map") && config.optJSONObject("app").optJSONObject("map").has("allowed_jurisdictions")) {
            JSONArray whitelist = config.optJSONObject("app").optJSONObject("map").optJSONArray("allowed_jurisdictions");
            jurisdictionAllowed = new ArrayList<>();
            for (int i = 0; i < whitelist.length(); i++) {
                jurisdictionAllowed.add(whitelist.optInt(i));
            }
        }
        setupSubscriptions(configuration);
    }

    public void configure(AirMapMapView.Configuration configuration) {
        configurationPublishSubject.onNext(configuration);
    }

    private void setupSubscriptions(AirMapMapView.Configuration configuration) {
        // observes changes to jurisdictions (map bounds) to query rulesets for the region
        Observable<Map<String, AirMapRuleset>> jurisdictionsObservable = jurisdictionsPublishSubject.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(polygon -> map != null && map.getMap() != null)
                .doOnNext(polygon -> {
                    if (callback != null) callback.onAdvisoryStatusLoading();
                })
                .flatMap(getJurisdictions())
                .filter(jurisdictions -> jurisdictions != null && !jurisdictions.isEmpty())
                .doOnNext(jurisdictions -> hasJurisdictions = true)
                .map(jurisdictions -> {
                    Map<String, AirMapRuleset> jurisdictionRulesets = new HashMap<>();
                    for (AirMapJurisdiction jurisdiction : jurisdictions) {
                        // If jurisdictionAllowed is null, it means no whitelist (allow all)
                        if (jurisdictionAllowed == null || jurisdictionAllowed.contains(jurisdiction.getId())) {
                            for (AirMapRuleset ruleset : jurisdiction.getRulesets()) {
                                Timber.v("AMMDC - Allowing: %s %s", jurisdiction.getId(), ruleset.getId());
                                jurisdictionRulesets.put(ruleset.getId(), ruleset);
                            }
                        }
                    }
                    Timber.i("Jurisdictions loaded: %s", TextUtils.join(",", jurisdictionRulesets.keySet()));

                    return new Pair<>(jurisdictionRulesets, jurisdictions);
                })
                .map(pair -> pair.first);

        // observes changes to preferred rulesets to trigger advisories fetch
        Observable<AirMapMapView.Configuration> configurationObservable = configurationPublishSubject
                .startWith(configuration)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<AirMapMapView.Configuration>() {
                    @Override
                    public void call(AirMapMapView.Configuration configuration) {
                        if (callback != null) {
                            callback.onAdvisoryStatusLoading();
                        }

                        switch (configuration.type) {
                            case AUTOMATIC:
                                Timber.i("AirMapMapView updated to automatic configuration");
                                break;
                            case DYNAMIC:
                                Timber.i("AirMapMapView updated to dynamic configuration w/ preferred rulesets: %s", TextUtils.join(",", ((AirMapMapView.DynamicConfiguration) configuration).preferredRulesetIds));
                                break;
                            case MANUAL:
                                Timber.i("AirMapMapView updated to manual configuration w/ preferred rulesets: %s", TextUtils.join(",", ((AirMapMapView.ManualConfiguration) configuration).selectedRulesets));
                                break;
                        }
                    }
                });

        // combines preferred rulesets and available rulesets changes
        // to calculate selected rulesets and advisories
        rulesetsSubscription = Observable.combineLatest(jurisdictionsObservable, configurationObservable,
                new Func2<Map<String, AirMapRuleset>, AirMapMapView.Configuration, Pair<List<AirMapRuleset>, List<AirMapRuleset>>>() {
                    @Override
                    public Pair<List<AirMapRuleset>, List<AirMapRuleset>> call(Map<String, AirMapRuleset> availableRulesetsMap, AirMapMapView.Configuration configuration) {
                        Timber.i("combine available rulesets & configuration");
                        List<AirMapRuleset> availableRulesets = new ArrayList<>(availableRulesetsMap.values());
                        List<AirMapRuleset> selectedRulesets = RulesetsEvaluator.computeSelectedRulesets(availableRulesets, configuration);

                        return new Pair<>(availableRulesets, selectedRulesets);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(rulesets -> rulesets != null)
                .doOnNext(pair -> {
                    Timber.i("Computed rulesets: %s", TextUtils.join(",", pair.second));
                    List<AirMapRuleset> availableRulesetsList = CopyCollections.copy(pair.first);
                    List<AirMapRuleset> selectedRulesetsList = CopyCollections.copy(pair.second);
                    List<AirMapRuleset> previouslySelectedRulesetsList = CopyCollections.copy(selectedRulesets);

                    // sort available rulesets
                    Collections.sort(availableRulesetsList, AirMapRuleset::compareTo);

                    callback.onRulesetsUpdated(availableRulesetsList, selectedRulesetsList, previouslySelectedRulesetsList);
                    availableRulesets = pair.first;
                    selectedRulesets = pair.second;
                })
                .map(pair -> pair.second)
                .filter(rulesets -> fetchAdvisories)
                .flatMap(convertRulesetsToAdvisories())
                .onErrorReturn(throwable -> {
                    Timber.e(throwable, "onErrorReturn");
                    return null;
                })
                .subscribe(advisoryStatus -> {
                    airspaceStatus = advisoryStatus;
                    callback.onAdvisoryStatusUpdated(advisoryStatus);
                }, throwable -> Timber.e(throwable, "Unknown error on jurisdictions observable"));
    }

    public void onMapLoaded() {
        jurisdictionsPublishSubject.onNext(null);
    }

    public void onMapRegionChanged() {
        jurisdictionsPublishSubject.onNextThrottled(null);
    }

    public void onMapFinishedRendering() {
        new Handler().postDelayed(() -> {
            if (!hasJurisdictions) {
                onMapLoaded();
            }
        }, 500);
    }

    protected Func1<AirMapPolygon, Observable<List<AirMapJurisdiction>>> getJurisdictions() {
        return polygon -> Observable.create((Observable.OnSubscribe<List<AirMapJurisdiction>>) subscriber -> {
            // query map for jurisdictions
            List<Feature> features = map.getMap().queryRenderedFeatures(new RectF(0,
                    0, map.getMeasuredWidth(), map.getMeasuredHeight()), "jurisdictions");

            if (features.isEmpty()) {
                Timber.d("Features are empty");
                hasJurisdictions = false;
                subscriber.onError(new Throwable("Features are empty"));
            }

            List<AirMapJurisdiction> jurisdictions = new ArrayList<>();
            for (Feature feature : features) {
                try {
                    JsonObject propertiesJSON = feature.properties();
                    JSONObject jurisdictionJSON = new JSONObject(propertiesJSON.get("jurisdiction").getAsString());

                    jurisdictions.add(new AirMapJurisdiction(jurisdictionJSON));
                } catch (JSONException e) {
                    Timber.e(e, "Unable to get jurisdiction json");
                }
            }
            subscriber.onNext(jurisdictions);
            subscriber.onCompleted();
        })
        .retryWhen(new RetryWithDelay(4, 400), AndroidSchedulers.mainThread())
        .onErrorReturn(throwable -> {
            Timber.v("Ran out of attempts to query jurisdictions");
            return null;
        });
    }

    /**
     * Fetches advisories based on map bounds and selected rulesets
     *
     * @return
     */
    private Func1<List<AirMapRuleset>, Observable<AirMapAirspaceStatus>> convertRulesetsToAdvisories() {
        return selectedRulesets -> getAdvisories(selectedRulesets, getPolygon())
                .onErrorResumeNext(throwable -> Observable.just(null));
    }

    protected AirMapPolygon getPolygon() {
        VisibleRegion region = map.getMap().getProjection().getVisibleRegion();
        LatLngBounds bounds = region.latLngBounds;

        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(bounds.getLatNorth(), bounds.getLonWest()));
        coordinates.add(new Coordinate(bounds.getLatNorth(), bounds.getLonEast()));
        coordinates.add(new Coordinate(bounds.getLatSouth(), bounds.getLonEast()));
        coordinates.add(new Coordinate(bounds.getLatSouth(), bounds.getLonWest()));
        coordinates.add(new Coordinate(bounds.getLatNorth(), bounds.getLonWest()));

        return new AirMapPolygon(coordinates);
    }

    private Observable<AirMapAirspaceStatus> getAdvisories(final List<AirMapRuleset> rulesets, final AirMapPolygon polygon) {
        return Observable.create(subscriber -> {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

            Date start = new Date();
            Date end = new Date(start.getTime() + (4 * 60 * 60 * 1000));

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

                start = cal1.getTime();
                end = cal2.getTime();
            }

            List<String> rulesetIds = new ArrayList<>();
            for (AirMapRuleset ruleset : rulesets) {
                rulesetIds.add(ruleset.getId());
            }

            final Call statusCall = AirMap.getAirspaceStatus(polygon, rulesetIds, start, end, new AirMapCallback<AirMapAirspaceStatus>() {
                @Override
                public void onSuccess(final AirMapAirspaceStatus response) {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                }

                @Override
                public void onError(AirMapException e) {
                    if (rulesets == null || rulesets.isEmpty()) {
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(e);
                    }
                }
            });

            subscriber.add(Subscriptions.create(statusCall::cancel));
        });
    }

    public List<AirMapAdvisory> getCurrentAdvisories() {
        return airspaceStatus == null ? null : CopyCollections.copy(airspaceStatus.getAdvisories());
    }

    public AirMapAirspaceStatus getAirspaceStatus() {
        return airspaceStatus;
    }

    public List<AirMapRuleset> getAvailableRulesets() {
        return CopyCollections.copy(availableRulesets);
    }

    public List<AirMapRuleset> getSelectedRulesets() {
        return CopyCollections.copy(selectedRulesets);
    }

    public AirMapMapView.Configuration getConfiguration() {
        return configuration;
    }

    public void disableAdvisories() {
        fetchAdvisories = false;
    }

    public void onMapReset() {
        availableRulesets = new ArrayList<>();
        selectedRulesets = new ArrayList<>();
    }

    public void onDestroy() {
        rulesetsSubscription.unsubscribe();
    }

    public interface Callback {
        void onRulesetsUpdated(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets, List<AirMapRuleset> previouslySelectedRulesets);

        void onAdvisoryStatusUpdated(AirMapAirspaceStatus advisoryStatus);

        void onAdvisoryStatusLoading();
    }
}
