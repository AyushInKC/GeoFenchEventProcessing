package com.AyushSinghAssingment.GeofenceEventProcessing.Repository;

import com.AyushSinghAssingment.GeofenceEventProcessing.Model.VehicleState;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VehicleRepository extends MongoRepository<VehicleState,String> {
    VehicleState findByVehicleId(String vehicleId);
}
