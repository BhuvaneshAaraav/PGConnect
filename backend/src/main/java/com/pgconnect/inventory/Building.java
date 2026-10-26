package com.pgconnect.inventory;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("buildings")
@CompoundIndexes({
        @CompoundIndex(name = "property_building_code", def = "{'propertyId': 1, 'codeNormalized': 1}", unique = true),
        @CompoundIndex(name = "owner_buildings", def = "{'ownerId': 1, 'propertyId': 1}")
})
public class Building {
    @Id private String id;
    private String ownerId;
    private String propertyId;
    private String name;
    private String code;
    private String codeNormalized;
    private int floorCount;
    private Instant createdAt = Instant.now();

    public Building() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getPropertyId() { return propertyId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public int getFloorCount() { return floorCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; this.codeNormalized = code == null ? null : code.trim().toLowerCase(); }
    public void setFloorCount(int floorCount) { this.floorCount = floorCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
