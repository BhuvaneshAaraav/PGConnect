package com.pgconnect.billing;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RentChargeRepository extends MongoRepository<RentCharge, String> {
    Optional<RentCharge> findByTenancyIdAndBillingPeriod(String tenancyId, String billingPeriod);
    List<RentCharge> findByResidentIdOrderByBillingPeriodDesc(String residentId);
    List<RentCharge> findByOwnerIdAndPropertyIdAndBillingPeriodOrderByBuildingIdAscRoomIdAsc(String ownerId, String propertyId, String billingPeriod);
    List<RentCharge> findByOwnerIdAndPropertyIdAndBillingPeriodAndOutstandingBalanceGreaterThanOrderByBuildingIdAscRoomIdAsc(String ownerId, String propertyId, String billingPeriod, long balance);
    Optional<RentCharge> findByIdAndResidentId(String id, String residentId);
    Optional<RentCharge> findByIdAndOwnerId(String id, String ownerId);
}
