package com.AyushSinghAssingment.GeofenceEventProcessing.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("zones")
public class Zone {
    @Id
    private String _id;
    private String id;
    private String name;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
}
