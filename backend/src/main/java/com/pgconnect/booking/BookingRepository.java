package com.pgconnect.booking;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByResidentIdOrderByCreatedAtDesc(String residentId);
    List<Booking> findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(String ownerId, String propertyId);
    Optional<Booking> findByIdAndOwnerIdAndPropertyId(String id, String ownerId, String propertyId);
    Optional<Booking> findByIdAndResidentId(String id, String residentId);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant before);
}
