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
        System.out.println("========= LOADING ZONES =========");
        System.out.println("DEBUG → Loaded Zones Count = " + cachedZones.size());

        for (Zone z : cachedZones) {
            System.out.println(
                    "ZONE LOADED → " + z.getName() +
                            " | Lat(" + z.getMinLat() + " → " + z.getMaxLat() + ")" +
                            " | Lon(" + z.getMinLon() + " → " + z.getMaxLon() + ")" +
                            " | ID = " + z.getId()
            );
        }
        System.out.println("=================================");
    }

    public EventResponse process(LocationEvent event) {

        System.out.println("\n========== PROCESSING EVENT ==========");
        System.out.println("VehicleId: " + event.getVehicleId() +
                " | Lat=" + event.getLat() +
                " | Lon=" + event.getLon() +
                " | Time=" + event.getTimestamp());

        VehicleState state = vehicleRepo.findByVehicleId(event.getVehicleId());
        System.out.println("DEBUG → Existing VehicleState = " + state);

        if (state == null) {
            System.out.println("DEBUG → Creating NEW VehicleState");
            state = new VehicleState(event.getVehicleId());
        }

        // Reject older timestamp event
        if (state.getLastTimestamp() != null &&
                state.getLastTimestamp().compareTo(event.getTimestamp()) > 0) {

            System.out.println("DEBUG → OLD timestamp, rejecting update");
            return new EventResponse(
                    event.getVehicleId(),
                    List.of(),
                    List.of(),
                    state.getCurrentZones()
            );
        }

        // Detect zones vehicle is inside
        List<String> newZones = cachedZones.stream()
                .filter(z -> GeoUtil.isInsideRectangle(
                        event.getLat(),
                        event.getLon(),
                        z.getMinLat(),
                        z.getMaxLat(),
                        z.getMinLon(),
                        z.getMaxLon()
                ))
                .map(Zone::getId)    // IMPORTANT: returns Mongo’s _id since @Id maps _id
                .toList();

        System.out.println("DEBUG → NEW ZONES DETECTED = " + newZones);

        List<String> oldZones = state.getCurrentZones() == null ? List.of() : state.getCurrentZones();
        System.out.println("DEBUG → OLD ZONES = " + oldZones);

        // Determine transitions
        List<String> entered = newZones.stream().filter(z -> !oldZones.contains(z)).toList();
        List<String> exited  = oldZones.stream().filter(z -> !newZones.contains(z)).toList();

        System.out.println("DEBUG → ENTERED = " + entered);
        System.out.println("DEBUG → EXITED  = " + exited);

        // Save enter/exit events
        for (String zone : entered) {
            eventRepo.save(new ZoneEvents(event.getVehicleId(), zone, "ENTER", event.getTimestamp()));
            System.out.println("DB EVENT SAVED → ENTER Zone = " + zone);
        }

        for (String zone : exited) {
            eventRepo.save(new ZoneEvents(event.getVehicleId(), zone, "EXIT", event.getTimestamp()));
            System.out.println("DB EVENT SAVED → EXIT Zone = " + zone);
        }

        // Update vehicle state
        state.setLat(event.getLat());
        state.setLon(event.getLon());
        state.setCurrentZones(newZones);
        state.setLastTimestamp(event.getTimestamp());

        vehicleRepo.save(state);
        System.out.println("DEBUG → VehicleState Updated & Saved");

        System.out.println("========= EVENT COMPLETED =========\n");

        return new EventResponse(event.getVehicleId(), entered, exited, newZones);
    }
}
