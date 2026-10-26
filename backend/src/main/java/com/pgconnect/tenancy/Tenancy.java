package com.pgconnect.tenancy;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document("tenancies")
@CompoundIndexes({
        @CompoundIndex(name = "owner_active_tenancies", def = "{'ownerId': 1, 'propertyId': 1, 'status': 1}"),
        @CompoundIndex(name = "resident_tenancies", def = "{'residentId': 1, 'status': 1, 'startDate': -1}")
})
public class Tenancy {
    @Id private String id;
    private String ownerId;
    private String residentId;
    private String propertyId;
    private String buildingId;
    private String roomId;
    private String bedId;
    private String bookingId;
    private long monthlyRent;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate endDate;
    private TenancyStatus status = TenancyStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Tenancy() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getResidentId() { return residentId; }
    public String getPropertyId() { return propertyId; }
    public String getBuildingId() { return buildingId; }
    public String getRoomId() { return roomId; }
    public String getBedId() { return bedId; }
    public String getBookingId() { return bookingId; }
    public long getMonthlyRent() { return monthlyRent; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public LocalDate getEndDate() { return endDate; }
    public TenancyStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setResidentId(String residentId) { this.residentId = residentId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setBedId(String bedId) { this.bedId = bedId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setMonthlyRent(long monthlyRent) { this.monthlyRent = monthlyRent; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(TenancyStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
