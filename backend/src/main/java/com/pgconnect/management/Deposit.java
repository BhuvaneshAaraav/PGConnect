package com.pgconnect.management;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("deposits")
public class Deposit {
    @Id private String id; @Indexed private String ownerId; @Indexed private String residentId; private String tenancyId; private String propertyId; private long amount; private long refundedAmount; private String status="HELD"; private String reference; private Instant receivedAt=Instant.now(); private Instant updatedAt=Instant.now();
    public String getId(){return id;} public String getOwnerId(){return ownerId;} public String getResidentId(){return residentId;} public String getTenancyId(){return tenancyId;} public String getPropertyId(){return propertyId;} public long getAmount(){return amount;} public long getRefundedAmount(){return refundedAmount;} public String getStatus(){return status;} public String getReference(){return reference;} public Instant getReceivedAt(){return receivedAt;}
    public void setOwnerId(String v){ownerId=v;} public void setResidentId(String v){residentId=v;} public void setTenancyId(String v){tenancyId=v;} public void setPropertyId(String v){propertyId=v;} public void setAmount(long v){amount=v;} public void setRefundedAmount(long v){refundedAmount=v;} public void setStatus(String v){status=v;} public void setReference(String v){reference=v;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
