package com.pgconnect.common;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document("audit_logs")
@CompoundIndex(name = "audit_entity_time", def = "{'entityType': 1, 'entityId': 1, 'createdAt': -1}")
public class AuditLog {
    @Id private String id;
    private String actorId;
    private String action;
    private String entityType;
    private String entityId;
    private Map<String, String> details;
    private Instant createdAt = Instant.now();
    public AuditLog() {}
    public AuditLog(String actorId, String action, String entityType, String entityId, Map<String, String> details) {
        this.actorId = actorId; this.action = action; this.entityType = entityType; this.entityId = entityId; this.details = details;
    }
    public String getId() { return id; }
    public String getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Map<String, String> getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
