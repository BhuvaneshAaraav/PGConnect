package com.pgconnect.inventory;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("rooms")
@CompoundIndexes({
        @CompoundIndex(name = "building_room_number", def = "{'buildingId': 1, 'roomNumberNormalized': 1}", unique = true),
        @CompoundIndex(name = "owner_property_rooms", def = "{'ownerId': 1, 'propertyId': 1, 'buildingId': 1}")
})
public class Room {
    @Id private String id;
    private String ownerId;
    private String propertyId;
    private String buildingId;
    private int floorNumber;
    private String roomNumber;
    private String roomNumberNormalized;
    private SharingType sharingType;
    private long monthlyRent;
    private List<String> facilities = new ArrayList<>();
    private RoomStatus status = RoomStatus.ACTIVE;
    private Instant createdAt = Instant.now();

    public Room() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getPropertyId() { return propertyId; }
    public String getBuildingId() { return buildingId; }
    public int getFloorNumber() { return floorNumber; }
    public String getRoomNumber() { return roomNumber; }
    public SharingType getSharingType() { return sharingType; }
    public long getMonthlyRent() { return monthlyRent; }
    public List<String> getFacilities() { return facilities; }
    public RoomStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; this.roomNumberNormalized = roomNumber == null ? null : roomNumber.trim().toLowerCase(); }
    public void setSharingType(SharingType sharingType) { this.sharingType = sharingType; }
    public void setMonthlyRent(long monthlyRent) { this.monthlyRent = monthlyRent; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities == null ? new ArrayList<>() : new ArrayList<>(facilities); }
    public void setStatus(RoomStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
