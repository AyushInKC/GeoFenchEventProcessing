package com.AyushSinghAssingment.GeofenceEventProcessing.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationEvent {
    @NotBlank
    private String vehicleId;
    @NotNull
    private Double lat;
    @NotNull private Double lon;
    @NotBlank private String timestamp;

}
