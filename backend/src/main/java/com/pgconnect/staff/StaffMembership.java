package com.pgconnect.staff;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document("staff_memberships")
@CompoundIndexes({
        @CompoundIndex(name = "property_staff_member", def = "{'propertyId':1,'staffUserId':1}", unique = true),
        @CompoundIndex(name = "staff_active_properties", def = "{'staffUserId':1,'active':1,'propertyId':1}"),
        @CompoundIndex(name = "owner_property_staff", def = "{'ownerId':1,'propertyId':1,'active':1}")
})
public class StaffMembership {
    @Id private String id;
    private String ownerId;
    private String propertyId;
    private String staffUserId;
    private Set<StaffPermission> permissions = new HashSet<>();
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getPropertyId() { return propertyId; }
    public String getStaffUserId() { return staffUserId; }
    public Set<StaffPermission> getPermissions() { return permissions; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setOwnerId(String value) { ownerId = value; }
    public void setPropertyId(String value) { propertyId = value; }
    public void setStaffUserId(String value) { staffUserId = value; }
    public void setPermissions(Set<StaffPermission> value) { permissions = value == null ? new HashSet<>() : new HashSet<>(value); }
    public void setActive(boolean value) { active = value; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
