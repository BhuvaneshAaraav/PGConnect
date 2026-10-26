package com.pgconnect.tenancy;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TenancyRepository extends MongoRepository<Tenancy, String> {
    List<Tenancy> findByOwnerIdAndPropertyIdAndStatusIn(String ownerId, String propertyId, List<TenancyStatus> statuses);
    List<Tenancy> findByResidentIdOrderByStartDateDesc(String residentId);
    Optional<Tenancy> findFirstByResidentIdAndStatusInOrderByStartDateDesc(String residentId, List<TenancyStatus> statuses);
    Optional<Tenancy> findByIdAndOwnerIdAndPropertyId(String id, String ownerId, String propertyId);
    boolean existsByResidentIdAndOwnerIdAndPropertyId(String residentId, String ownerId, String propertyId);
}
