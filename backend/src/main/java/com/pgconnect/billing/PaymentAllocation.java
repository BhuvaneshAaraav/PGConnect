package com.pgconnect.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("payment_allocations")
public class PaymentAllocation {
    @Id private String id;
    @Indexed private String paymentId;
    @Indexed private String chargeId;
    private long amount;
    private Instant allocatedAt = Instant.now();
    public PaymentAllocation() {}
    public PaymentAllocation(String paymentId, String chargeId, long amount) { this.paymentId = paymentId; this.chargeId = chargeId; this.amount = amount; }
    public String getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public String getChargeId() { return chargeId; }
    public long getAmount() { return amount; }
    public Instant getAllocatedAt() { return allocatedAt; }
}
