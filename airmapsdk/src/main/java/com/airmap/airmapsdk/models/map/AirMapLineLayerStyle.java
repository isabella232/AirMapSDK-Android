package com.airmap.airmapsdk.models.map;

import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapLineLayerStyle extends AirMapLayerStyle {

    AirMapLineLayerStyle(JSONObject json) {
        super(json);
    }

    @Override
    public LineLayer toMapboxLayer(Layer layerToClone, String sourceId) {
        LineLayer lineLayer = new LineLayer(id + "|" + sourceId + "|new", sourceId);
        lineLayer.setSourceLayer(sourceId + "_" + sourceLayer);

        LineLayer layer = (LineLayer) layerToClone;
        List<PropertyValue> properties = new ArrayList<>();

        PropertyValue lineColor = getLineColor(layer.getLineColor());
        if (lineColor != null) {
            properties.add(lineColor);
        }

        PropertyValue lineOpacity = getLineOpacity(layer.getLineOpacity());
        if (lineOpacity != null) {
            properties.add(lineOpacity);
        }

        PropertyValue lineWidth = getLineWidth(layer.getLineWidth());
        if (lineWidth != null) {
            properties.add(lineWidth);
        }

        PropertyValue lineGapWidth = getLineGapWidth(layer.getLineGapWidth());
        if (lineGapWidth != null) {
            properties.add(lineGapWidth);
        }

        PropertyValue lineTranslate = getLineTranslate(layer.getLineTranslate());
        if (lineTranslate != null) {
            properties.add(lineTranslate);
        }

        PropertyValue lineTranslateAnchor = getLineTranslateAnchor(layer.getLineTranslateAnchor());
        if (lineTranslateAnchor != null) {
            properties.add(lineTranslateAnchor);
        }

        PropertyValue lineDashArray = getLineDashArray(layer.getLineDasharray());
        if (lineDashArray != null) {
            properties.add(lineDashArray);
        }

        PropertyValue linePattern = getLinePattern(layer.getLinePattern());
        if (linePattern != null) {
            properties.add(linePattern);
        }

        // set all properties
        lineLayer.setProperties(properties.toArray(new PropertyValue[properties.size()]));

        if (layer.getFilter() != null) {
            lineLayer.setFilter(layer.getFilter());
        }

        return lineLayer;
    }

    private PropertyValue getLineColor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineColor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineColor(pv.getColorInt());
        }
        return null;
    }

    private PropertyValue getLineOpacity(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineOpacity(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineOpacity((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLineWidth(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineWidth(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineWidth((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLineGapWidth(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineGapWidth(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineGapWidth((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLineTranslate(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineTranslate(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineTranslate((Float[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLineTranslateAnchor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineTranslateAnchor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineTranslateAnchor((String) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLineDashArray(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.lineDasharray(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.lineDasharray((Float[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getLinePattern(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.linePattern(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.linePattern((String) pv.getValue());
        }
        return null;
    }
}
