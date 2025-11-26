package com.AyushSinghAssingment.GeofenceEventProcessing.Service;

import com.AyushSinghAssingment.GeofenceEventProcessing.DTOs.EventResponse;
import com.AyushSinghAssingment.GeofenceEventProcessing.DTOs.LocationEvent;
import com.AyushSinghAssingment.GeofenceEventProcessing.Geo.GeoUtil;
import com.AyushSinghAssingment.GeofenceEventProcessing.Model.VehicleState;
import com.AyushSinghAssingment.GeofenceEventProcessing.Model.Zone;
import com.AyushSinghAssingment.GeofenceEventProcessing.Model.ZoneEvents;
import com.AyushSinghAssingment.GeofenceEventProcessing.Repository.VehicleRepository;
import com.AyushSinghAssingment.GeofenceEventProcessing.Repository.ZoneEventRepository;
import com.AyushSinghAssingment.GeofenceEventProcessing.Repository.ZoneRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class EventService {

    private final VehicleRepository vehicleRepo;
    private final ZoneRepository zoneRepo;
    private final ZoneEventRepository eventRepo;

    private List<Zone> cachedZones;

    @PostConstruct
    public void loadZones() {
        cachedZones = zoneRepo.findAll();
        System.out.println("DEBUG → Loaded Zones Count = " + cachedZones.size());
    }

    public EventResponse process(LocationEvent event) {

        // ❗ FIX: Load by vehicleId, NOT by _id
        VehicleState state = vehicleRepo.findByVehicleId(event.getVehicleId());

        if (state == null) {
            state = new VehicleState(event.getVehicleId());
        }


        // Reject old timestamps
        if (state.getLastTimestamp() != null &&
                state.getLastTimestamp().compareTo(event.getTimestamp()) > 0) {
            return new EventResponse(
                    event.getVehicleId(),
                    List.of(),
                    List.of(),
                    state.getCurrentZones()
            );
        }

        // Zone detection
        List<String> newZones = cachedZones.stream()
                .filter(z -> GeoUtil.isInsideRectangle(
                        event.getLat(),
                        event.getLon(),
                        z.getMinLat(),
                        z.getMaxLat(),
                        z.getMinLon(),
                        z.getMaxLon()
                ))
                .map(Zone::getId)
                .toList();

        List<String> oldZones = state.getCurrentZones() == null ? List.of() : state.getCurrentZones();

        // Transitions
        List<String> entered = newZones.stream().filter(z -> !oldZones.contains(z)).toList();
        List<String> exited = oldZones.stream().filter(z -> !newZones.contains(z)).toList();

        // Save enter and exit
        for (String zone : entered)
            eventRepo.save(new ZoneEvents(event.getVehicleId(), zone, "ENTER", event.getTimestamp()));

        for (String zone : exited)
            eventRepo.save(new ZoneEvents(event.getVehicleId(), zone, "EXIT", event.getTimestamp()));

        // Update vehicle state
        state.setLat(event.getLat());
        state.setLon(event.getLon());
        state.setCurrentZones(newZones);
        state.setLastTimestamp(event.getTimestamp());

        vehicleRepo.save(state);

        return new EventResponse(event.getVehicleId(), entered, exited, newZones);
    }
}
