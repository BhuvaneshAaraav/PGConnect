package com.pgconnect.booking;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("bookings")
@CompoundIndexes({
        @CompoundIndex(name = "resident_booking_history", def = "{'residentId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "owner_booking_queue", def = "{'ownerId': 1, 'status': 1, 'createdAt': -1}")
})
public class Booking {
    @Id private String id;
    private String residentId;
    private String ownerId;
    private String propertyId;
    private String buildingId;
    private String roomId;
    private String bedId;
    private BookingStatus status = BookingStatus.REQUESTED;
    private String residentMessage;
    private String ownerResponse;
    private Instant expiresAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Booking() {}
    public String getId() { return id; }
    public String getResidentId() { return residentId; }
    public String getOwnerId() { return ownerId; }
    public String getPropertyId() { return propertyId; }
    public String getBuildingId() { return buildingId; }
    public String getRoomId() { return roomId; }
    public String getBedId() { return bedId; }
    public BookingStatus getStatus() { return status; }
    public String getResidentMessage() { return residentMessage; }
    public String getOwnerResponse() { return ownerResponse; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setId(String id) { this.id = id; }
    public void setResidentId(String residentId) { this.residentId = residentId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setBedId(String bedId) { this.bedId = bedId; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setResidentMessage(String residentMessage) { this.residentMessage = residentMessage; }
    public void setOwnerResponse(String ownerResponse) { this.ownerResponse = ownerResponse; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
