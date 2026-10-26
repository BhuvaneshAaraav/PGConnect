package com.pgconnect.management;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("complaints")
@CompoundIndexes({@CompoundIndex(name="resident_complaints",def="{'residentId':1,'createdAt':-1}"),@CompoundIndex(name="owner_complaints",def="{'ownerId':1,'propertyId':1,'status':1,'createdAt':-1}")})
public class Complaint {
    @Id private String id; private String residentId; private String ownerId; private String tenancyId; private String propertyId;
    private String buildingId; private String roomId; private String bedId; private String category; private String title; private String description;
    private ComplaintStatus status=ComplaintStatus.OPEN; private String ownerResponse; private String assignedTo;
    private Instant createdAt=Instant.now(); private Instant updatedAt=Instant.now(); private Instant resolvedAt;
    public String getId(){return id;} public String getResidentId(){return residentId;} public String getOwnerId(){return ownerId;} public String getTenancyId(){return tenancyId;}
    public String getPropertyId(){return propertyId;} public String getBuildingId(){return buildingId;} public String getRoomId(){return roomId;} public String getBedId(){return bedId;}
    public String getCategory(){return category;} public String getTitle(){return title;} public String getDescription(){return description;} public ComplaintStatus getStatus(){return status;}
    public String getOwnerResponse(){return ownerResponse;} public String getAssignedTo(){return assignedTo;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getResolvedAt(){return resolvedAt;}
    public void setResidentId(String v){residentId=v;} public void setOwnerId(String v){ownerId=v;} public void setTenancyId(String v){tenancyId=v;} public void setPropertyId(String v){propertyId=v;}
    public void setBuildingId(String v){buildingId=v;} public void setRoomId(String v){roomId=v;} public void setBedId(String v){bedId=v;} public void setCategory(String v){category=v;} public void setTitle(String v){title=v;} public void setDescription(String v){description=v;}
    public void setStatus(ComplaintStatus v){status=v;} public void setOwnerResponse(String v){ownerResponse=v;} public void setAssignedTo(String v){assignedTo=v;} public void setUpdatedAt(Instant v){updatedAt=v;} public void setResolvedAt(Instant v){resolvedAt=v;}
}
