package com.AyushSinghAssingment.GeofenceEventProcessing.Model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
@Data
@Document("vehicles")
public class VehicleState {
    @Id
    private String id;
    private String vehicleId;
    private double lat;
    private double lon;
    private String lastTimestamp;
    private List<String> currentZones;

    public VehicleState() {}

    public VehicleState(String vehicleId) {
        this.vehicleId = vehicleId;
        this.currentZones = List.of();
    }

}
