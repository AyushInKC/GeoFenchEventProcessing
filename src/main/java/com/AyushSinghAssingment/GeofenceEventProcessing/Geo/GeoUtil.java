package com.AyushSinghAssingment.GeofenceEventProcessing.Geo;

public class GeoUtil {
    public static boolean isInsideRectangle(
            double lat, double lon,
            double minLat, double maxLat,
            double minLon, double maxLon) {

        return lat >= minLat && lat <= maxLat &&
                lon >= minLon && lon <= maxLon;
    }
}
