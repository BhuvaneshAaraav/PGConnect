package com.pgconnect.inventory;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BuildingRepository extends MongoRepository<Building, String> {
    List<Building> findByOwnerIdAndPropertyIdOrderByCode(String ownerId, String propertyId);
    boolean existsByOwnerIdAndPropertyIdAndId(String ownerId, String propertyId, String id);
}
