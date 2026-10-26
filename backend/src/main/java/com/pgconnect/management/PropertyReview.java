package com.pgconnect.management;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("reviews")
@CompoundIndex(name="resident_property_review",def="{'residentId':1,'propertyId':1}",unique=true)
public class PropertyReview {
    @Id private String id; private String residentId; private String ownerId; private String propertyId; private String tenancyId; private int rating; private String text; private String status="PUBLISHED"; private String reportReason; private Instant createdAt=Instant.now(); private Instant updatedAt=Instant.now();
    public String getId(){return id;} public String getResidentId(){return residentId;} public String getOwnerId(){return ownerId;} public String getPropertyId(){return propertyId;} public String getTenancyId(){return tenancyId;} public int getRating(){return rating;} public String getText(){return text;} public String getStatus(){return status;} public String getReportReason(){return reportReason;} public Instant getCreatedAt(){return createdAt;}
    public void setResidentId(String v){residentId=v;} public void setOwnerId(String v){ownerId=v;} public void setPropertyId(String v){propertyId=v;} public void setTenancyId(String v){tenancyId=v;} public void setRating(int v){rating=v;} public void setText(String v){text=v;} public void setStatus(String v){status=v;} public void setReportReason(String v){reportReason=v;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
