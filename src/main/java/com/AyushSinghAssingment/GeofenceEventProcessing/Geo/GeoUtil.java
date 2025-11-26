package com.AyushSinghAssingment.GeofenceEventProcessing.Geo;

import com.AyushSinghAssingment.GeofenceEventProcessing.Model.Zone;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

public class GeoUtil {
    public static boolean isInsideRectangle(
            double lat, double lon,
            double minLat, double maxLat,
            double minLon, double maxLon) {

        return lat >= minLat && lat <= maxLat &&
                lon >= minLon && lon <= maxLon;
    }
}
