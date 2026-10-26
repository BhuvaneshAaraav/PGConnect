package com.pgconnect.property;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PgPropertyRepository extends MongoRepository<PgProperty, String> {
    List<PgProperty> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    java.util.Optional<PgProperty> findByIdAndOwnerId(String id, String ownerId);
    long countByStatus(PropertyStatus status);
}
