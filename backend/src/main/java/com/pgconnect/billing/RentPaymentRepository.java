package com.pgconnect.billing;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RentPaymentRepository extends MongoRepository<RentPayment, String> {
    Optional<RentPayment> findByIdempotencyKey(String idempotencyKey);
    List<RentPayment> findByResidentIdOrderByConfirmedAtDesc(String residentId);
    List<RentPayment> findByChargeIdOrderByConfirmedAtDesc(String chargeId);
    Optional<RentPayment> findByIdAndResidentId(String id, String residentId);
    Optional<RentPayment> findByIdAndOwnerId(String id, String ownerId);
}
