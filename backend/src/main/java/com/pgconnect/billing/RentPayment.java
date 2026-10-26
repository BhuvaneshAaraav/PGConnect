package com.pgconnect.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("rent_payments")
@CompoundIndex(name = "resident_payment_history", def = "{'residentId': 1, 'confirmedAt': -1}")
public class RentPayment {
    @Id private String id;
    private String ownerId;
    private String residentId;
    private String tenancyId;
    private String chargeId;
    private String propertyId;
    private long amount;
    private PaymentMethod method;
    private RentPaymentStatus status = RentPaymentStatus.CONFIRMED;
    private String reference;
    private String recordedBy;
    @Indexed(unique = true) private String idempotencyKey;
    private String gatewayPaymentId;
    private boolean gatewayVerified;
    private Instant confirmedAt = Instant.now();
    private String receiptNumber;

    public RentPayment() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getResidentId() { return residentId; }
    public String getTenancyId() { return tenancyId; }
    public String getChargeId() { return chargeId; }
    public String getPropertyId() { return propertyId; }
    public long getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public RentPaymentStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public String getRecordedBy() { return recordedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public boolean isGatewayVerified() { return gatewayVerified; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setResidentId(String residentId) { this.residentId = residentId; }
    public void setTenancyId(String tenancyId) { this.tenancyId = tenancyId; }
    public void setChargeId(String chargeId) { this.chargeId = chargeId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public void setStatus(RentPaymentStatus status) { this.status = status; }
    public void setReference(String reference) { this.reference = reference; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }
    public void setGatewayVerified(boolean gatewayVerified) { this.gatewayVerified = gatewayVerified; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
}
