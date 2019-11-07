package com.airmap.airmapsdk.models.map;

import com.mapbox.mapboxsdk.style.layers.Layer;
import org.json.JSONObject;


import static com.airmap.airmapsdk.util.Utils.optString;

public abstract class AirMapLayerStyle {

    public final String id;
    public final String source;
    public final String sourceLayer;
    public final String type;
    public final float minZoom;

    AirMapLayerStyle(JSONObject json) {
        id = optString(json, "id");
        source = optString(json, "source");
        sourceLayer = optString(json, "source-layer");
        type = optString(json, "type");
        minZoom = (float) json.optDouble("minzoom", 0);
    }

    public abstract Layer toMapboxLayer(Layer layerToClone, String sourceId);

    static AirMapLayerStyle fromJson(JSONObject jsonObject) {
        String type = optString(jsonObject, "type");

        switch (type) {
            case "fill": {
                return new AirMapFillLayerStyle(jsonObject);
            }
            case "line": {
                return new AirMapLineLayerStyle(jsonObject);
            }
            case "symbol": {
                return new AirMapSymbolLayerStyle(jsonObject);
            }
            default: {
                return null;
            }
        }
    }
}
