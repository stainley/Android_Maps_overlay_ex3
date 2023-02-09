package ca.app.assasins.exerciseoverlaymap.util;

import android.annotation.SuppressLint;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

public class DistanceUtil {


    public String showDistance(LatLng mMarkerA, LatLng mMarkerB) {
        double distance = SphericalUtil.computeDistanceBetween(mMarkerA, mMarkerB);
        return formatNumber(distance);
    }

    @SuppressLint("DefaultLocale")
    public String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }

        return String.format("%4.3f%s", distance, unit);
    }

}
