package com.AyushSinghAssingment.GeofenceEventProcessing.Repository;

import com.AyushSinghAssingment.GeofenceEventProcessing.Model.Zone;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ZoneRepository extends MongoRepository<Zone,String> {


}
