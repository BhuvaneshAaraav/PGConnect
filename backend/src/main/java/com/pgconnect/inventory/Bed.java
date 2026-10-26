package com.pgconnect.inventory;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("beds")
@CompoundIndexes({
        @CompoundIndex(name = "room_bed_code", def = "{'roomId': 1, 'bedCodeNormalized': 1}", unique = true),
        @CompoundIndex(name = "property_bed_status", def = "{'propertyId': 1, 'status': 1}"),
        @CompoundIndex(name = "owner_room_beds", def = "{'ownerId': 1, 'buildingId': 1, 'roomId': 1}")
})
public class Bed {
    @Id private String id;
    private String ownerId;
    private String propertyId;
    private String buildingId;
    private String roomId;
    private String bedCode;
    private String bedCodeNormalized;
    private BedStatus status = BedStatus.AVAILABLE;
    private String bookingId;
    private String tenancyId;
    private Instant reservedUntil;
    private Instant createdAt = Instant.now();

    public Bed() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getPropertyId() { return propertyId; }
    public String getBuildingId() { return buildingId; }
    public String getRoomId() { return roomId; }
    public String getBedCode() { return bedCode; }
    public BedStatus getStatus() { return status; }
    public String getBookingId() { return bookingId; }
    public String getTenancyId() { return tenancyId; }
    public Instant getReservedUntil() { return reservedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setBedCode(String bedCode) { this.bedCode = bedCode; this.bedCodeNormalized = bedCode == null ? null : bedCode.trim().toLowerCase(); }
    public void setStatus(BedStatus status) { this.status = status; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setTenancyId(String tenancyId) { this.tenancyId = tenancyId; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
