package com.pgconnect.staff;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.common.AuditLog;
import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StaffAccessService {
    private final StaffMembershipRepository memberships;
    private final UserAccountRepository users;
    private final PgPropertyRepository properties;
    private final MongoTemplate mongo;

    public StaffAccessService(StaffMembershipRepository memberships, UserAccountRepository users,
                              PgPropertyRepository properties, MongoTemplate mongo) {
        this.memberships = memberships;
        this.users = users;
        this.properties = properties;
        this.mongo = mongo;
    }

    public List<StaffMemberView> ownerMembers(String ownerId, String propertyId) {
        requireOwnerProperty(ownerId, propertyId);
        return memberships.findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(ownerId, propertyId).stream().map(this::view).toList();
    }

    public StaffMemberView addMember(String ownerId, String propertyId, AddMemberRequest request) {
        requireOwnerProperty(ownerId, propertyId);
        UserAccount staff = users.findByEmailNormalized(request.email().trim().toLowerCase())
                .filter(user -> user.getRole() == Role.STAFF && user.isEnabled() && user.isEmailVerified())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The staff member must register as staff and verify their email before you can grant access"));
        StaffMembership membership = memberships.findByStaffUserIdAndPropertyId(staff.getId(), propertyId).orElseGet(StaffMembership::new);
        if (membership.getId() != null && membership.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This staff account already has access to this property");
        }
        membership.setOwnerId(ownerId);
        membership.setPropertyId(propertyId);
        membership.setStaffUserId(staff.getId());
        membership.setPermissions(request.permissions());
        membership.setActive(true);
        membership.setUpdatedAt(Instant.now());
        try {
            StaffMembership saved = memberships.save(membership);
            audit(ownerId, "STAFF_ACCESS_GRANTED", saved, request.permissions().toString());
            return view(saved);
        } catch (DuplicateKeyException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This staff account already has access to this property");
        }
    }

    public StaffMemberView updateMember(String ownerId, String propertyId, String membershipId, UpdateMemberRequest request) {
        requireOwnerProperty(ownerId, propertyId);
        StaffMembership membership = memberships.findByOwnerIdAndPropertyIdAndId(ownerId, propertyId, membershipId)
                .orElseThrow(() -> notFound("Staff membership not found"));
        if (request.active() && request.permissions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one permission for active staff access");
        }
        membership.setPermissions(request.permissions());
        membership.setActive(request.active());
        membership.setUpdatedAt(Instant.now());
        StaffMembership saved = memberships.save(membership);
        audit(ownerId, request.active() ? "STAFF_ACCESS_UPDATED" : "STAFF_ACCESS_REVOKED", saved, request.permissions().toString());
        return view(saved);
    }

    public void revokeMember(String ownerId, String propertyId, String membershipId) {
        requireOwnerProperty(ownerId, propertyId);
        StaffMembership membership = memberships.findByOwnerIdAndPropertyIdAndId(ownerId, propertyId, membershipId)
                .orElseThrow(() -> notFound("Staff membership not found"));
        membership.setActive(false);
        membership.setPermissions(Set.of());
        membership.setUpdatedAt(Instant.now());
        memberships.save(membership);
        audit(ownerId, "STAFF_ACCESS_REVOKED", membership, "Membership removed");
    }

    public List<StaffPropertyView> staffProperties(String staffUserId) {
        UserAccount staff = users.findById(staffUserId).filter(UserAccount::isEnabled)
                .filter(user -> user.getRole() == Role.STAFF).orElse(null);
        if (staff == null) return List.of();
        return memberships.findByStaffUserIdAndActiveTrueOrderByCreatedAtDesc(staffUserId).stream()
                .filter(membership -> users.findById(membership.getOwnerId()).filter(UserAccount::isEnabled).isPresent())
                .map(membership -> properties.findByIdAndOwnerId(membership.getPropertyId(), membership.getOwnerId())
                        .map(property -> staffProperty(membership, property)).orElse(null))
                .filter(view -> view != null).toList();
    }

    public String requireStaffOwner(String staffUserId, String propertyId, StaffPermission permission) {
        users.findById(staffUserId).filter(UserAccount::isEnabled).filter(user -> user.getRole() == Role.STAFF)
                .orElseThrow(() -> notFound("Property not found"));
        StaffMembership membership = memberships.findByStaffUserIdAndPropertyId(staffUserId, propertyId)
                .filter(StaffMembership::isActive)
                .filter(value -> value.getPermissions().contains(permission))
                .orElseThrow(() -> notFound("Property not found"));
        UserAccount owner = users.findById(membership.getOwnerId()).filter(UserAccount::isEnabled)
                .orElseThrow(() -> notFound("Property not found"));
        if (owner.getRole() != Role.OWNER || properties.findByIdAndOwnerId(propertyId, owner.getId()).isEmpty()) {
            throw notFound("Property not found");
        }
        return owner.getId();
    }

    private StaffPropertyView staffProperty(StaffMembership membership, PgProperty property) {
        return new StaffPropertyView(property.getId(), property.getName(), property.getLocality(), property.getCity(),
                membership.getPermissions());
    }

    private StaffMemberView view(StaffMembership membership) {
        UserAccount staff = users.findById(membership.getStaffUserId()).orElse(null);
        return new StaffMemberView(membership.getId(), membership.getStaffUserId(),
                staff == null ? "Staff account removed" : staff.getName(), staff == null ? null : staff.getEmailNormalized(),
                membership.getPermissions(), membership.isActive(), membership.getCreatedAt());
    }

    private void requireOwnerProperty(String ownerId, String propertyId) {
        UserAccount owner = users.findById(ownerId).filter(u -> u.getRole() == Role.OWNER && u.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner account required"));
        properties.findByIdAndOwnerId(propertyId, owner.getId()).orElseThrow(() -> notFound("Property not found"));
    }

    private void audit(String actorId, String action, StaffMembership membership, String permissions) {
        mongo.save(new AuditLog(actorId, action, "StaffMembership", membership.getId(),
                Map.of("propertyId", membership.getPropertyId(), "staffUserId", membership.getStaffUserId(), "permissions", permissions)));
    }

    private static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record AddMemberRequest(@jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
                                   @jakarta.validation.constraints.NotEmpty Set<StaffPermission> permissions) {}
    public record UpdateMemberRequest(@jakarta.validation.constraints.NotNull Set<StaffPermission> permissions, boolean active) {}
    public record StaffMemberView(String id, String userId, String name, String email, Set<StaffPermission> permissions,
                                  boolean active, Instant createdAt) {}
    public record StaffPropertyView(String propertyId, String propertyName, String locality, String city,
                                    Set<StaffPermission> permissions) {}
}
