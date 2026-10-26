package com.pgconnect.admin;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.index.CompoundIndex;import org.springframework.data.mongodb.core.mapping.Document;import java.time.Instant;
@Document("property_reports") @CompoundIndex(name="report_queue",def="{'status':1,'createdAt':-1}")
public class PropertyReport {
 @Id private String id;private String propertyId;private String ownerId;private String reporterId;private String reason;private String details;private String status="OPEN";private String adminResponse;private String reviewedBy;private Instant createdAt=Instant.now();private Instant reviewedAt;
 public String getId(){return id;}public String getPropertyId(){return propertyId;}public String getOwnerId(){return ownerId;}public String getReporterId(){return reporterId;}public String getReason(){return reason;}public String getDetails(){return details;}public String getStatus(){return status;}public String getAdminResponse(){return adminResponse;}public String getReviewedBy(){return reviewedBy;}public Instant getCreatedAt(){return createdAt;}public Instant getReviewedAt(){return reviewedAt;}
 public void setPropertyId(String v){propertyId=v;}public void setOwnerId(String v){ownerId=v;}public void setReporterId(String v){reporterId=v;}public void setReason(String v){reason=v;}public void setDetails(String v){details=v;}public void setStatus(String v){status=v;}public void setAdminResponse(String v){adminResponse=v;}public void setReviewedBy(String v){reviewedBy=v;}public void setReviewedAt(Instant v){reviewedAt=v;}
}
