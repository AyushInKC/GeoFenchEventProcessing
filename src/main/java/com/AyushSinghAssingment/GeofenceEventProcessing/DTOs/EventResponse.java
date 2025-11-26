package com.AyushSinghAssingment.GeofenceEventProcessing.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {
    private String vehicleId;
    private List<String> entered;
    private List<String> exited;
    private List<String> currentZones;

    public EventResponse(boolean b) {
    }

    public EventResponse(List<String> entered, List<String> exited, List<String> newZones) {
    }
}
