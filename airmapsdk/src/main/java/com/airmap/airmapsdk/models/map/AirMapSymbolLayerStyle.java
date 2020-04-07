package com.airmap.airmapsdk.models.map;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AirMapSymbolLayerStyle extends AirMapLayerStyle {

    AirMapSymbolLayerStyle(@NonNull JSONObject json) {
        super(json);
    }

    @Override
    public SymbolLayer toMapboxLayer(Layer layerToClone, String sourceId) {
        SymbolLayer symbolLayer = new SymbolLayer(id + "|" + sourceId + "|new", sourceId);
        symbolLayer.setSourceLayer(sourceId + "_" + sourceLayer);

        SymbolLayer layer = (SymbolLayer) layerToClone;
        List<PropertyValue> properties = new ArrayList<>();

        PropertyValue symbolPlacement = getSymbolPlacement(layer.getSymbolPlacement());
        if (symbolPlacement != null) {
            properties.add(symbolPlacement);
        }

        PropertyValue symbolSpacing = getSymbolSpacing(layer.getSymbolSpacing());
        if (symbolSpacing != null) {
            properties.add(symbolSpacing);
        }

        PropertyValue iconImage = getIconImage(layer.getIconImage());
        if (iconImage != null) {
            properties.add(iconImage);
        }

        PropertyValue iconAllowOverlap = getIconAllowOverlap(layer.getIconAllowOverlap());
        if (iconAllowOverlap != null) {
            properties.add(iconAllowOverlap);
        }

        PropertyValue iconKeepUpright = getIconKeepUpright(layer.getIconKeepUpright());
        if (iconKeepUpright != null) {
            properties.add(iconKeepUpright);
        }

        PropertyValue avoidEdges = getAvoidEdges(layer.getSymbolAvoidEdges());
        if (avoidEdges != null) {
            properties.add(avoidEdges);
        }

        PropertyValue iconPadding = getIconPadding(layer.getIconPadding());
        if (iconPadding != null) {
            properties.add(iconPadding);
        }

        PropertyValue iconSize = getIconSize(layer.getIconSize());
        if (iconSize != null) {
            properties.add(iconSize);
        }

        PropertyValue iconOpacity = getIconOpacity(layer.getIconOpacity());
        if (iconOpacity != null) {
            properties.add(iconOpacity);
        }

        PropertyValue iconOffset = getIconOffset(layer.getIconOffset());
        if (iconOffset != null) {
            properties.add(iconOffset);
        }

        PropertyValue textField = getTextField(layer.getTextField());
        if (textField != null) {
            properties.add(textField);
        }

        PropertyValue textFont = getTextFont(layer.getTextFont());
        if (textFont != null) {
            properties.add(textFont);
        }

        PropertyValue textOffset = getTextOffset(layer.getTextOffset());
        if (textOffset != null) {
            properties.add(textOffset);
        }

        PropertyValue textColor = getTextColor(layer.getTextColor());
        if (textColor != null) {
            properties.add(textColor);
        }

        PropertyValue textPadding = getTextPadding(layer.getTextPadding());
        if (textPadding != null) {
            properties.add(textPadding);
        }

        PropertyValue textOpacity = getTextOpacity(layer.getTextOpacity());
        if (textOpacity != null) {
            properties.add(textOpacity);
        }

        // set all properties
        symbolLayer.setProperties(properties.toArray(new PropertyValue[properties.size()]));

        if (layer.getFilter() != null) {
            symbolLayer.setFilter(layer.getFilter());
        }

        if (symbolLayer.getMaxZoom() != 0) {
            symbolLayer.setMaxZoom(symbolLayer.getMaxZoom());
        }

        if (symbolLayer.getMinZoom() != 0) {
            symbolLayer.setMinZoom(symbolLayer.getMinZoom());
        }

        return symbolLayer;
    }

    private PropertyValue getSymbolPlacement(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.symbolPlacement(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.symbolPlacement((String) pv.getValue());
        }
        return null;
    }

    private PropertyValue getSymbolSpacing(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.symbolSpacing(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.symbolSpacing((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconAllowOverlap(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconAllowOverlap(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconAllowOverlap((Boolean) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconKeepUpright(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconKeepUpright(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconKeepUpright((Boolean) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconImage(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconImage(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconImage((String) pv.getValue());
        }
        return null;
    }

    private PropertyValue getAvoidEdges(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.symbolAvoidEdges(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.symbolAvoidEdges((Boolean) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconPadding(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconPadding(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconPadding((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconSize(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconSize(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconSize((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconOpacity(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconOpacity(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconOpacity((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getIconOffset(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.iconOffset(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.iconOffset((Float[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getTextField(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textField(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textField((String) pv.getValue());
        }
        return null;
    }

    private PropertyValue getTextFont(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textFont(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textFont((String[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getTextOffset(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textOffset(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textOffset((Float[]) pv.getValue());
        }
        return null;
    }

    private PropertyValue getTextColor(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textColor(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textColor(pv.getColorInt());
        }
        return null;
    }

    private PropertyValue getTextPadding(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textPadding(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textPadding((Float) pv.getValue());
        }
        return null;
    }

    private PropertyValue getTextOpacity(PropertyValue pv) {
        if (pv.isExpression()) {
            return PropertyFactory.textOpacity(pv.getExpression());
        } else if (pv.isValue()) {
            return PropertyFactory.textOpacity((Float) pv.getValue());
        }
        return null;
    }
}
