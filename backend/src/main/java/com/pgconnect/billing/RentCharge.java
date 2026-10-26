package com.pgconnect.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document("rent_charges")
@CompoundIndexes({
        @CompoundIndex(name = "tenancy_billing_period", def = "{'tenancyId': 1, 'billingPeriod': 1}", unique = true),
        @CompoundIndex(name = "owner_charge_report", def = "{'ownerId': 1, 'propertyId': 1, 'billingPeriod': 1, 'status': 1}"),
        @CompoundIndex(name = "resident_charge_history", def = "{'residentId': 1, 'billingPeriod': -1}")
})
public class RentCharge {
    @Id private String id;
    private String ownerId;
    private String residentId;
    private String tenancyId;
    private String propertyId;
    private String buildingId;
    private String roomId;
    private String bedId;
    private String billingPeriod;
    private long rentAmount;
    private long paidAmount;
    private long outstandingBalance;
    private LocalDate dueDate;
    private RentChargeStatus status = RentChargeStatus.UNPAID;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public RentCharge() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getResidentId() { return residentId; }
    public String getTenancyId() { return tenancyId; }
    public String getPropertyId() { return propertyId; }
    public String getBuildingId() { return buildingId; }
    public String getRoomId() { return roomId; }
    public String getBedId() { return bedId; }
    public String getBillingPeriod() { return billingPeriod; }
    public long getRentAmount() { return rentAmount; }
    public long getPaidAmount() { return paidAmount; }
    public long getOutstandingBalance() { return outstandingBalance; }
    public LocalDate getDueDate() { return dueDate; }
    public RentChargeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setResidentId(String residentId) { this.residentId = residentId; }
    public void setTenancyId(String tenancyId) { this.tenancyId = tenancyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setBedId(String bedId) { this.bedId = bedId; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
    public void setRentAmount(long rentAmount) { this.rentAmount = rentAmount; }
    public void setPaidAmount(long paidAmount) { this.paidAmount = paidAmount; }
    public void setOutstandingBalance(long outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setStatus(RentChargeStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
