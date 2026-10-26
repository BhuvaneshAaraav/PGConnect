package com.pgconnect.subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface OwnerSubscriptionRepository extends MongoRepository<OwnerSubscription,String> {
    Optional<OwnerSubscription> findFirstByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Optional<OwnerSubscription> findByProviderSubscriptionId(String subscriptionId);
    List<OwnerSubscription> findByStatusInAndCurrentPeriodEndBefore(List<OwnerSubscriptionStatus> statuses, java.time.Instant time);
    List<OwnerSubscription> findByCancelAtCycleEndTrueAndStatusInAndCurrentPeriodEndBefore(List<OwnerSubscriptionStatus> statuses, java.time.Instant time);
    List<OwnerSubscription> findByStatusAndCancelAtCycleEndFalseAndCurrentPeriodEndBetween(OwnerSubscriptionStatus status, java.time.Instant start, java.time.Instant end);
}
