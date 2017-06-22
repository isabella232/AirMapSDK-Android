package com.airmap.airmapsdk.models.flight;

import android.text.TextUtils;
import android.util.Log;

import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraft;
import com.airmap.airmapsdk.models.pilot.AirMapPilot;
import com.airmap.airmapsdk.models.shapes.AirMapGeometry;
import com.airmap.airmapsdk.models.shapes.AirMapPath;
import com.airmap.airmapsdk.models.shapes.AirMapPoint;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.airmap.airmapsdk.util.Utils.getDateFromIso8601String;
import static com.airmap.airmapsdk.util.Utils.getIso8601StringFromDate;

/**
 * Created by collin@airmap.com on 5/19/17.
 */

public class AirMapFlightPlan implements Serializable, AirMapBaseModel {

    private String planId;
    private String pilotId;
    private String aircraftId;
    private Date startsAt;
    private Date endsAt;
    private float buffer;
    private String geometry;
    private float maxAltitude;
    private Coordinate coordinate;
    private List<String> rulesetIds;
    private Map<String, FlightFeatureValue> flightFeatureValues;

    private Date createdAt;
    private boolean isPublic;
    private boolean notify;
    private List<String> permitIds;

    public AirMapFlightPlan() {

    }

    public AirMapFlightPlan(JSONObject flightJson) {
        constructFromJson(flightJson);
    }

    @Override
    public AirMapFlightPlan constructFromJson(JSONObject json) {
        if (json != null) {
            setPlanId(json.optString("id"));
            setCoordinate(new Coordinate(json.optDouble("latitude", 0), json.optDouble("longitude", 0)));
            setMaxAltitude((float) json.optDouble("max_altitude_agl"));
            setNotify(json.optBoolean("notify"));
            setPilotId(json.optString("pilot_id"));
            setAircraftId(json.optString("aircraft_id", null));
            setPublic(json.optBoolean("public"));
            setBuffer((float) json.optDouble("buffer"));

            JSONObject geometryGeoJSON = json.optJSONObject("geometry");
            if (geometryGeoJSON != null) {
                setGeometry(geometryGeoJSON.toString());
            }

            rulesetIds = new ArrayList<>();
            JSONArray rulesetsJson = json.optJSONArray("rulesets");
            for (int i = 0; rulesetsJson != null && i < rulesetsJson.length(); i++) {
                rulesetIds.add(rulesetsJson.optString(i));
            }

            permitIds = new ArrayList<>();
            JSONArray permitIdsJson = json.optJSONArray("permits");
            for (int i = 0; permitIdsJson != null && i < permitIdsJson.length(); i++) {
                addPermitId(permitIdsJson.optString(i));
            }

            flightFeatureValues = new HashMap<>();
            JSONObject flightFeaturesJSON = json.optJSONObject("flight_features");
            if (flightFeaturesJSON != null) {
                Iterator<String> featuresItr = flightFeaturesJSON.keys();
                while (featuresItr.hasNext()) {
                    String key = featuresItr.next();
                    Object value = flightFeaturesJSON.opt(key);
                    FlightFeatureValue flightFeatureValue = new FlightFeatureValue(key, value);
                    flightFeatureValues.put(key, flightFeatureValue);
                }
            }

            //Created at
            if (json.has("created_at")) {
                setCreatedAt(getDateFromIso8601String(json.optString("created_at")));
            } else if (json.has("creation_date")) {
                setCreatedAt(getDateFromIso8601String(json.optString("creation_date")));
            }

            String startTime = json.optString("start_time");
            if (!TextUtils.isEmpty(startTime) && !startTime.equals("now")) {
                setStartsAt(getDateFromIso8601String(startTime));
            }
            setEndsAt(getDateFromIso8601String(json.optString("end_time")));
        }
        return this;
    }

    /**
     * Turn the AirMapFlight into a Map of it's fields and values for easy use with web calls
     *
     * @return The AirMapFlight encoded as a Map
     */
    public JSONObject getAsParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", getPlanId());
        params.put("pilot_id", getPilotId());

        if (getAircraftId() != null && !getAircraftId().isEmpty()) {
            params.put("aircraft_id", getAircraftId());
        }

        if (getStartsAt() != null && getStartsAt().after(new Date()) && getIso8601StringFromDate(getStartsAt()) != null) {
            params.put("start_time", getIso8601StringFromDate(getStartsAt()));
        } else {
            params.put("start_time", "now");
        }
        params.put("end_time", getIso8601StringFromDate(getEndsAt()));

        params.put("geometry", getGeometry().toString());

        params.put("buffer", buffer);

        params.put("max_altitude_agl", getMaxAltitude());
        Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) { //Remove any null values
            Map.Entry<String, Object> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue().equals("null")) {
                iterator.remove();
            }
        }
        JSONObject jsonObject = new JSONObject(params);
        if (getPermitIds() != null && !getPermitIds().isEmpty()) {
            JSONArray permitsArray = new JSONArray(getPermitIds());
            try {
                jsonObject.put("permits", permitsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (rulesetIds != null && !rulesetIds.isEmpty()) {
            JSONArray rulesetsArray = new JSONArray(rulesetIds);
            try {
                jsonObject.put("rulesets", rulesetsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (flightFeatureValues != null && !flightFeatureValues.isEmpty()) {
            JSONObject flightFeaturesJson = new JSONObject();
            for (FlightFeatureValue featureValue : flightFeatureValues.values()) {
                try {
                    flightFeaturesJson.put(featureValue.getKey(), featureValue.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try {
                jsonObject.put("flight_features", flightFeaturesJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    /**
     * @param coordinates The coordinates to make a polygon from
     * @return A GeoJSON formatted String
     */
    public static String getPolygonString(List<Coordinate> coordinates) {
        return new AirMapPolygon(coordinates).toString();
    }

    /**
     * @param coordinates The coordinates to make a line of
     * @return a GeoJSON formatted String
     */
    public static String getLineString(List<Coordinate> coordinates) {
        return new AirMapPath(coordinates).toString();
    }

    /**
     * @param coordinate The coordinate to make the WKT string for
     * @return a GeoJSON formatted String
     */
    public static String getPointString(Coordinate coordinate) {
        return new AirMapPoint(coordinate).toString();
    }

    /**
     * Determines whether the current AirMapFlightPlan is valid or not
     *
     * @return The validity of the flight
     */
    public boolean isValid() {
        return planId != null;
    }

    /**
     * Determines whether the AirMapFlightPlan is a currently active flight
     *
     * @return Whether the flight is current or not
     */
    public boolean isActive() {
        Date now = new Date();
        return startsAt != null && endsAt != null && now.after(startsAt) && now.before(endsAt);
    }

    public String getPlanId() {
        return planId;
    }

    public String getPilotId() {
        return pilotId;
    }

    public AirMapFlightPlan setPilotId(String pilotId) {
        this.pilotId = pilotId;
        return this;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public AirMapFlightPlan setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    /**
     * @return maxAltitude of the flight in meters
     */
    public double getMaxAltitude() {
        return maxAltitude;
    }

    /**
     * @param maxAltitude Altitude of the flight in meters
     */
    public AirMapFlightPlan setMaxAltitude(float maxAltitude) {
        this.maxAltitude = maxAltitude;
        return this;
    }

    /**
     * @return buffer, in meters
     */
    public double getBuffer() {
        return buffer;
    }

    public AirMapFlightPlan setPlanId(String planId) {
        this.planId = planId;
        return this;
    }

    /**
     * @param buffer buffer, in meters
     */
    public AirMapFlightPlan setBuffer(float buffer) {
        this.buffer = buffer;
        return this;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public AirMapFlightPlan setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public AirMapFlightPlan setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getStartsAt() {
        return startsAt;
    }

    public AirMapFlightPlan setStartsAt(Date startsAt) {
        this.startsAt = startsAt;
        return this;
    }

    public Date getEndsAt() {
        return endsAt;
    }

    public AirMapFlightPlan setEndsAt(Date endsAt) {
        this.endsAt = endsAt;
        return this;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public AirMapFlightPlan setPublic(boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public boolean shouldNotify() {
        return notify;
    }

    public AirMapFlightPlan setNotify(boolean notify) {
        this.notify = notify;
        return this;
    }

    public List<String> getPermitIds() {
        return permitIds;
    }

    public AirMapFlightPlan setPermitIds(ArrayList<String> permitIds) {
        this.permitIds = permitIds;
        return this;
    }

    /**
     * @param id A PilotPermit Id (should start with "permit_application")
     */
    public AirMapFlightPlan addPermitId(String id) {
        permitIds.add(id);
        return this;
    }

    public String getGeometry() {
        return geometry;
    }

    public AirMapFlightPlan setGeometry(String geometry) {
        this.geometry = geometry;
        return this;
    }

    public List<String> getRulesetIds() {
        return rulesetIds;
    }

    public void setRulesetIds(List<String> rulesetIds) {
        this.rulesetIds = rulesetIds;
    }

    public Map<String, FlightFeatureValue> getFlightFeatureValues() {
        return flightFeatureValues;
    }

    public void setFlightFeatureValues(Map<String, FlightFeatureValue> flightFeatureValues) {
        this.flightFeatureValues = flightFeatureValues;
    }

    public void setFlightFeatureValue(FlightFeatureValue flightFeatureValue) {
        if (flightFeatureValues == null) {
            flightFeatureValues = new HashMap<>();
        } else {
            if (flightFeatureValues.containsKey(flightFeatureValue.getKey())) {
                flightFeatureValues.remove(flightFeatureValue.getKey());
            }
        }

        if (flightFeatureValue.getValue() != null) {
            flightFeatureValues.put(flightFeatureValue.getKey(), flightFeatureValue);
        }
    }

    /**
     * Comparison based on ID
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AirMapFlightPlan && getPlanId().equals(((AirMapFlightPlan) o).getPlanId());
    }
}