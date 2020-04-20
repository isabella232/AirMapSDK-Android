package com.airmap.airmapsdk.networking.services;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.util.AirMapConfig;
import com.airmap.airmapsdk.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import timber.log.Timber;

@SuppressWarnings("unused")
public class MappingService extends BaseService {

    public enum AirMapAirspaceType {
        Airport("airport", R.string.airspace_type_airport),
        Heliport("heliport", R.string.airspace_type_heliport),
        Park("park", R.string.airspace_type_national_park),
        PowerPlant("power_plant", R.string.airspace_type_power_plant),
        ControlledAirspace("controlled_airspace", R.string.airspace_type_controlled),
        School("school", R.string.airspace_type_school),
        SpecialUse("special_use_airspace", R.string.airspace_type_special_use),
        SeaplaneBase("seaplane_base", R.string.airspace_type_seaplane_base),
        TFR("tfr", R.string.airspace_type_tfr_faa),
        Wildfires("wildfire", R.string.airspace_type_wildfire),
        Fires("fire", R.string.airspace_type_fire),
        Emergencies("emergency", R.string.airspace_type_emergency),
        Hospitals("hospital", R.string.airspace_type_hospital),
        HazardArea("hazard_area", R.string.airspace_type_hazard_area),
        RecreationalArea("recreational_area", R.string.airspace_type_aerial_rec_area),
        City("city", R.string.airspace_type_city),
        Custom("custom", R.string.airspace_type_custom),
        Prison("prison", R.string.airspace_type_prison),
        University("university", R.string.airspace_type_university),
        Notam("notam", R.string.airspace_type_notam),
        AMA("ama_field", R.string.airspace_type_ama),
        County("county", R.string.county),
        Country("country", R.string.country),
        Embassy("embassy", R.string.embassy),
        FIR("fir", R.string.fir),
        FederalBuilding("federal_building", R.string.federal_building),
        GliderPort("gliderport", R.string.glider_port),
        Highway("highway", R.string.highway),
        IndustrialProperty("industrial_property", R.string.industrial_property),
        MilitaryProperty("military_property", R.string.military_property),
        PoliceStation("police_station", R.string.police_station),
        Powerline("powerline", R.string.powerline),
        Railway("railway", R.string.railway),
        ResidentialProperty("residential_property", R.string.residential_property),
        Stadium("stadium", R.string.stadium),
        State("state", R.string.state),
        Subprefecture("subprefecture", R.string.subprefecture),
        Supercity("supercity", R.string.supercity),
        UlmField("ulm_field", R.string.ulm_field),
        Waterway("waterway", R.string.waterway),
        JapanBase("jpn_base", R.string.japan_base_admin),
        Notification("notification", R.string.airspace_type_notification),
        TMA("tma", R.string.airspace_type_tma),
        LandingSite("landing_site", R.string.airspace_type_landing_site),
        Unknown("unknown", R.string.airspace_type_unknown);

        private final String text;
        private final int title;

        AirMapAirspaceType(String text, @StringRes int title) {
            this.text = text;
            this.title = title;
        }

        @Override
        public String toString() {
            return text;
        }

        public int getTitle() {
            return title;
        }

        public static AirMapAirspaceType fromString(String text) {
            switch (text) {
                case "airport":
                    return Airport;
                case "heliport":
                    return Heliport;
                case "park":
                    return Park;
                case "power_plant":
                    return PowerPlant;
                case "controlled_airspace":
                    return ControlledAirspace;
                case "school":
                    return School;
                case "special_use_airspace":
                    return SpecialUse;
                case "tfr":
                    return TFR;
                case "wildfire":
                    return Wildfires;
                case "fire":
                    return Fires;
                case "emergency":
                    return Emergencies;
                case "hospital":
                    return Hospitals;
                case "hazard_area":
                    return HazardArea;
                case "recreational_area":
                    return RecreationalArea;
                case "city":
                    return City;
                case "custom":
                    return Custom;
                case "prison":
                    return Prison;
                case "university":
                    return University;
                case "seaplane_base":
                    return SeaplaneBase;
                case "notam":
                    return Notam;
                case "notification":
                    return Notification;
                case "ama":
                case "ama_field":
                    return AMA;
                case "country":
                    return Country;
                case "county":
                    return County;
                case "embassy":
                    return Embassy;
                case "fir":
                    return FIR;
                case "federal_building":
                    return FederalBuilding;
                case "gliderport":
                    return GliderPort;
                case "highway":
                    return Highway;
                case "industrial_property":
                    return IndustrialProperty;
                case "landing_site":
                    return LandingSite;
                case "military_property":
                    return MilitaryProperty;
                case "police_station":
                    return PoliceStation;
                case "powerline":
                    return Powerline;
                case "railway":
                    return Railway;
                case "residential_property":
                    return ResidentialProperty;
                case "stadium":
                    return Stadium;
                case "state":
                    return State;
                case "subprefecture":
                    return Subprefecture;
                case "supercity":
                    return Supercity;
                case "tma":
                    return TMA;
                case "ulm_field":
                    return UlmField;
                case "waterway":
                    return Waterway;
                default:
                    return Unknown;
            }
        }
    }

    public enum AirMapMapTheme {
        Standard("standard"),
        Dark("dark"),
        Light("light"),
        Satellite("satellite");

        private final String text;

        AirMapMapTheme(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static AirMapMapTheme fromString(String text) {
            switch (text) {
                case "standard":
                case "street":
                    return Standard;
                case "light":
                    return Light;
                case "dark":
                    return Dark;
                case "satellite":
                    return Satellite;
            }
            return null;
        }
    }

    protected String getRulesetTileUrlTemplate(String rulesetId, List<String> layers, boolean useSIMeasurements, @Nullable String accessToken) {
        String units = "?units=" + (useSIMeasurements ? "si" : "airmap");
        String url = mapTilesRulesUrl + "/" + rulesetId + "/" + TextUtils.join(",", layers) + "/{z}/{x}/{y}" + units;
        if (!TextUtils.isEmpty(accessToken)) {
            url += "&access_token=" + accessToken;
        }
        return url;
    }

    protected String getBaseJurisdictionsUrlTemplate() {
        return mapTilesBaseJurisdictionsUrl;
    }

    protected String getStylesUrl(AirMapMapTheme theme) {
        String stylesUrl = AirMapConfig.getMapStyleUrl();

        // fallback
        if (TextUtils.isEmpty(stylesUrl)) {
            stylesUrl = "https://cdn.airmap.com/static/map-styles/0.10.0-beta1/";
        }

        switch (theme) {
            case Light:
                stylesUrl += "light.json";
                break;
            case Dark:
                stylesUrl += "dark.json";
                break;
            case Satellite:
                stylesUrl += "satellite.json";
                break;
            default:
            case Standard:
                stylesUrl += "standard.json";
                break;
        }

        return stylesUrl;
    }

    protected Call getStylesJson(AirMapMapTheme theme, final AirMapCallback<JSONObject> listener) {
        return AirMap.getClient().get(getStylesUrl(theme), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.error(new AirMapException(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString;
                try {
                    jsonString = response.body().string();
                } catch (IOException e) {
                    Utils.error(listener, e);
                    return;
                }
                response.body().close();
                JSONObject result = null;
                try {
                    result = new JSONObject(jsonString);
                    listener.success(result);
                } catch (JSONException e) {
                    Timber.e(e, "Unable to parse map style:%s", jsonString);
                    listener.error(new AirMapException(e.getMessage()));
                }
            }
        });
    }
}
