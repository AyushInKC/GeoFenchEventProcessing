package com.AyushSinghAssingment.GeofenceEventProcessing.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document("zones")
public class Zone {
    @Id
    private String id;
    private String name;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
}
