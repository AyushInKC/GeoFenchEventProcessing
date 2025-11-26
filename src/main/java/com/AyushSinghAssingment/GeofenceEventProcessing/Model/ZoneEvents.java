package com.AyushSinghAssingment.GeofenceEventProcessing.Model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("zoneEvents")
public class ZoneEvents {
    private String vehicleId;
    private String zoneId;
    private String transition; // Enter or Exit
    private String timestamp;
    public ZoneEvents(String vehicleId, String zoneId, String transition, String timestamp) {
        this.vehicleId = vehicleId;
        this.zoneId = zoneId;
        this.transition = transition;
        this.timestamp = timestamp;
    }

}
