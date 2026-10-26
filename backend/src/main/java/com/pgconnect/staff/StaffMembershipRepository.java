package com.pgconnect.staff;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StaffMembershipRepository extends MongoRepository<StaffMembership, String> {
    List<StaffMembership> findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(String ownerId, String propertyId);
    Optional<StaffMembership> findByOwnerIdAndPropertyIdAndId(String ownerId, String propertyId, String id);
    Optional<StaffMembership> findByStaffUserIdAndPropertyId(String staffUserId, String propertyId);
    List<StaffMembership> findByStaffUserIdAndActiveTrueOrderByCreatedAtDesc(String staffUserId);
    boolean existsByStaffUserIdAndActiveTrue(String staffUserId);
}
