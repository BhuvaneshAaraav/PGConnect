package com.pgconnect.inventory;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BedRepository extends MongoRepository<Bed, String> {
    List<Bed> findByOwnerIdAndPropertyIdOrderByBuildingIdAscRoomIdAscBedCodeAsc(String ownerId, String propertyId);
    List<Bed> findByOwnerIdAndPropertyIdAndBuildingIdOrderByRoomIdAscBedCodeAsc(String ownerId, String propertyId, String buildingId);
    List<Bed> findByOwnerIdAndPropertyIdAndBuildingIdAndRoomIdOrderByBedCodeAsc(String ownerId, String propertyId, String buildingId, String roomId);
    List<Bed> findByRoomIdOrderByBedCodeAsc(String roomId);
    void deleteByRoomId(String roomId);
    Optional<Bed> findByIdAndOwnerIdAndPropertyId(String id, String ownerId, String propertyId);
    List<Bed> findByStatusAndReservedUntilBefore(BedStatus status, Instant before);
    long countByOwnerIdAndPropertyId(String ownerId, String propertyId);
    long countByOwnerIdAndPropertyIdAndStatus(String ownerId, String propertyId, BedStatus status);
    long countByPropertyIdAndStatus(String propertyId, BedStatus status);
    List<Bed> findByPropertyIdAndStatus(String propertyId, BedStatus status);
    List<Bed> findByOwnerIdInAndStatus(java.util.Collection<String> ownerIds, BedStatus status);
}
