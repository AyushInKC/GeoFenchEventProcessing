package com.AyushSinghAssingment.GeofenceEventProcessing.Controller;

import com.AyushSinghAssingment.GeofenceEventProcessing.DTOs.EventResponse;
import com.AyushSinghAssingment.GeofenceEventProcessing.DTOs.LocationEvent;
import com.AyushSinghAssingment.GeofenceEventProcessing.Model.VehicleState;
import com.AyushSinghAssingment.GeofenceEventProcessing.Repository.VehicleRepository;
import com.AyushSinghAssingment.GeofenceEventProcessing.Repository.ZoneEventRepository;
import com.AyushSinghAssingment.GeofenceEventProcessing.Service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final VehicleRepository vehicleRepo;
    private final ZoneEventRepository zoneEventRepo;
    public EventController(EventService eventService,
                           VehicleRepository vehicleRepo,
                           ZoneEventRepository zoneEventRepo) {
        this.eventService = eventService;
        this.vehicleRepo = vehicleRepo;
        this.zoneEventRepo = zoneEventRepo;
    }


    // POST /api/events → Process GPS event

    @PostMapping("/events")

    public ResponseEntity<EventResponse> receive(@Valid @RequestBody LocationEvent event) {
        System.out.println(eventService.process(event));
        return ResponseEntity.ok(eventService.process(event));

    }


    //GET /api/vehicles/{id} → Get current zone status

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<?> getVehicleState(@PathVariable String id) {
        VehicleState state = vehicleRepo.findById(id).orElse(null);
        if (state == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(state);
    }

    // GET /api/zones/events/{id} → Get past ENTER/EXIT logs for vehicle
    @GetMapping("/zones/events/{id}")
    public ResponseEntity<?> getVehicleEvents(@PathVariable String id) {
        return ResponseEntity.ok(zoneEventRepo.findByVehicleId(id));
    }
}
