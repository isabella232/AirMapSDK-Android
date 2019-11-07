package com.airmap.airmapsdk.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.util.Utils;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public abstract class Container {

    public static final String POINT_LAYER = "flight-plan-point-layer";
    public static final String POINT_SOURCE = "flight-plan-point-source";
    public static final String MIDPOINT_LAYER = "flight-plan-midpoint-layer";
    public static final String MIDPOINT_SOURCE = "flight-plan-midpoint-source";
    public static final String INTERSECTION_LAYER = "flight-plan-intersection-layer";
    public static final String INTERSECTION_SOURCE = "flight-plan-intersection-source";
    public static final String POLYGON_LAYER = "flight-plan-polygon-layer";
    public static final String POLYGON_SOURCE = "flight-plan-polygon-source";
    public static final String POLYLINE_LAYER = "flight-plan-polyline-layer";
    public static final String POLYLINE_SOURCE = "flight-plan-polyline-source";

    protected static final String CORNER_IMAGE = "corner-img";
    protected static final String MIDPOINT_IMAGE = "midpoint-img";
    protected static final String INTERSECTION_IMAGE = "intersection-img";

    protected Context context;
    protected MapboxMap map;

    public Container(Context context, MapboxMap map) {
        this.context = context;
        this.map = map;

        map.getStyle().addImage(CORNER_IMAGE, Utils.getBitmapForDrawable(context, R.drawable.white_circle));
        map.getStyle().addImage(MIDPOINT_IMAGE, Utils.getBitmapForDrawable(context, R.drawable.gray_circle));
        map.getStyle().addImage(INTERSECTION_IMAGE, Utils.getBitmapForDrawable(context, R.drawable.intersection_circle));
    }

    protected List<Point> latLngsToPositions(List<LatLng> latLngs) {
        List<Point> positions = new ArrayList<>();
        for (LatLng latLng : latLngs) {
            positions.add(Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
        }
        return positions;
    }

    public final void zoomTo() {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int smallestWidth = Math.min(dm.widthPixels, dm.heightPixels);
        int padding = smallestWidth / 5;
        int topPadding = padding * 2;

        zoomTo(padding, topPadding, padding, topPadding);
    }

    public final void zoomTo(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        try {
            Analytics.logDebug("latlngbound", getLatLngBoundsForZoom().toString());
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBoundsForZoom(), paddingLeft, paddingTop, paddingRight, paddingBottom), 200);
        } catch (Error err) {
            Analytics.logDebug("Container padding", paddingLeft + ", " + paddingTop + " , " + paddingRight + ", " + paddingBottom);
            Analytics.logDebug("Map padding", Arrays.toString(map.getPadding()));
            Analytics.report(new Exception("Latitude/Longitude must not be NaN: " + getLatLngBoundsForZoom().toString(), err));
            Timber.e(err);
        }
    }

    public abstract void clear();

    protected abstract LatLngBounds getLatLngBoundsForZoom();

    public abstract boolean isValid();
}
