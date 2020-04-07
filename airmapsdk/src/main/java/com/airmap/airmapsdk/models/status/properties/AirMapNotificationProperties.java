package com.airmap.airmapsdk.models.status.properties;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

import static com.airmap.airmapsdk.util.Utils.optString;

@SuppressWarnings("unused")
public class AirMapNotificationProperties implements Serializable, AirMapBaseModel {
    private String url;
    private String body;

    /**
     * Initialize an AirMapNotificationProperties from JSON
     *
     * @param propertiesJson The JSON representation
     */
    public AirMapNotificationProperties(JSONObject propertiesJson) {
        constructFromJson(propertiesJson);
    }

    public AirMapNotificationProperties() {

    }

    @Override
    public AirMapNotificationProperties constructFromJson(JSONObject json) {
        if (json != null) {
            setUrl(optString(json, "url"));
            setBody(optString(json, "body"));
        }
        return this;
    }

    public String getUrl() {
        return url;
    }

    public AirMapNotificationProperties setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
