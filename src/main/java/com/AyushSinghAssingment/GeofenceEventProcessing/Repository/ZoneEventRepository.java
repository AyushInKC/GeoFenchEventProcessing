package com.AyushSinghAssingment.GeofenceEventProcessing.Repository;

import com.AyushSinghAssingment.GeofenceEventProcessing.Model.ZoneEvents;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ZoneEventRepository extends MongoRepository<ZoneEvents,String> {


    Object findByVehicleId(String id);
}
