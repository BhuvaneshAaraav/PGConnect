package com.pgconnect.management;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("announcements")
@CompoundIndex(name="property_announcements",def="{'propertyId':1,'publishedAt':-1}")
public class Announcement {
    @Id private String id; private String ownerId; private String propertyId; private String buildingId; private String residentId; private String title; private String body; private boolean published=true; private Instant publishedAt=Instant.now(); private Instant expiresAt;
    public String getId(){return id;} public String getOwnerId(){return ownerId;} public String getPropertyId(){return propertyId;} public String getBuildingId(){return buildingId;} public String getResidentId(){return residentId;} public String getTitle(){return title;} public String getBody(){return body;} public boolean isPublished(){return published;} public Instant getPublishedAt(){return publishedAt;} public Instant getExpiresAt(){return expiresAt;}
    public void setOwnerId(String v){ownerId=v;} public void setPropertyId(String v){propertyId=v;} public void setBuildingId(String v){buildingId=v;} public void setResidentId(String v){residentId=v;} public void setTitle(String v){title=v;} public void setBody(String v){body=v;} public void setPublished(boolean v){published=v;} public void setExpiresAt(Instant v){expiresAt=v;}
}
