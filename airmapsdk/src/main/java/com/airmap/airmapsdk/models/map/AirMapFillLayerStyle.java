package com.airmap.airmapsdk.models.map;

import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AirMapFillLayerStyle extends AirMapLayerStyle {

    AirMapFillLayerStyle(JSONObject json) {
        super(json);
    }

    @Override
    public FillLayer toMapboxLayer(Layer layerToClone, String sourceId) {
        FillLayer fillLayer = new FillLayer(id + "|" + sourceId + "|new", sourceId);
        fillLayer.setSourceLayer(sourceId + "_" + sourceLayer);

        FillLayer layer = (FillLayer) layerToClone;
        List<PropertyValue> properties = new ArrayList<>();

        PropertyValue fillColor = getFillColor(layer.getFillColor());
        if (fillColor != null) {
            properties.add(fillColor);
        }

        PropertyValue fillOpacity = getFillOpacity(layer.getFillOpacity());
        if (fillOpacity != null) {
            properties.add(fillOpacity);
        }

        PropertyValue fillAntialias = getFillAntialias(layer.getFillAntialias());
        if (fillAntialias != null) {
            properties.add(fillAntialias);
        }

        PropertyValue fillOutlineColor = getFillOutlineColor(layer.getFillOutlineColor());
        if (fillOutlineColor != null) {
            properties.add(fillOutlineColor);
        }

        PropertyValue fillTranslate = getFillTranslate(layer.getFillTranslate());
        if (fillTranslate != null) {
            properties.add(fillTranslate);
        }

        PropertyValue fillTranslateAnchor = getFillTranslateAnchor(layer.getFillTranslateAnchor());
        if (fillTranslateAnchor != null) {
            properties.add(fillTranslateAnchor);
        }

        PropertyValue fillPattern = getFillPattern(layer.getFillPattern());
        if (fillPattern != null) {
            properties.add(fillPattern);
        }

        // set all properties
        fillLayer.setProperties(properties.toArray(new PropertyValue[properties.size()]));

        if (layer.getFilter() != null) {
            fillLayer.setFilter(layer.getFilter());
        }

        return fillLayer;
    }

    private PropertyValue getFillColor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillColor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillColor(pv.getColorInt());
        }
        return null;
    }

    private PropertyValue getFillOpacity(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillOpacity(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillOpacity((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getFillAntialias(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillAntialias(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillAntialias((Boolean) pv.getValue());
        }
        return null;
    }

    private PropertyValue getFillOutlineColor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillOutlineColor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillOutlineColor(pv.getColorInt());
        }
        return null;
    }

    private PropertyValue getFillTranslate(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillTranslate(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillTranslate((Float[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getFillTranslateAnchor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillTranslateAnchor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillTranslateAnchor((String) pv.getValue());
        }
        return null;
    }

    private PropertyValue getFillPattern(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.fillPattern(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.fillPattern((String) pv.getValue());
        }
        return null;
    }
}
