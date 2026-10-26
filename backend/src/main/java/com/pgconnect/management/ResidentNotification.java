package com.pgconnect.management;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("notifications")
@CompoundIndex(name="user_notifications",def="{'userId':1,'readAt':1,'createdAt':-1}")
public class ResidentNotification {
    @Id private String id; private String userId; private String type; private String title; private String message; private String entityType; private String entityId; private Instant createdAt=Instant.now(); private Instant readAt;
    public String getId(){return id;} public String getUserId(){return userId;} public String getType(){return type;} public String getTitle(){return title;} public String getMessage(){return message;} public String getEntityType(){return entityType;} public String getEntityId(){return entityId;} public Instant getCreatedAt(){return createdAt;} public Instant getReadAt(){return readAt;}
    public void setUserId(String v){userId=v;} public void setType(String v){type=v;} public void setTitle(String v){title=v;} public void setMessage(String v){message=v;} public void setEntityType(String v){entityType=v;} public void setEntityId(String v){entityId=v;} public void setReadAt(Instant v){readAt=v;}
}
