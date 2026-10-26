package com.pgconnect.inventory;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByPropertyIdAndSharingType(String propertyId, SharingType sharingType);
    List<Room> findByOwnerIdInAndSharingType(java.util.Collection<String> ownerIds, SharingType sharingType);
    List<Room> findByPropertyIdOrderByBuildingIdAscFloorNumberAscRoomNumberAsc(String propertyId);
    List<Room> findByOwnerIdAndPropertyIdOrderByBuildingIdAscFloorNumberAscRoomNumberAsc(String ownerId, String propertyId);
    List<Room> findByOwnerIdAndPropertyIdAndBuildingIdOrderByFloorNumberAscRoomNumberAsc(String ownerId, String propertyId, String buildingId);
    boolean existsByOwnerIdAndPropertyIdAndBuildingIdAndId(String ownerId, String propertyId, String buildingId, String id);
}
