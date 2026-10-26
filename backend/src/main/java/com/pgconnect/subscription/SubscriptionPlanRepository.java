package com.pgconnect.subscription;
import org.springframework.data.mongodb.repository.MongoRepository;import java.util.Optional;
public interface SubscriptionPlanRepository extends MongoRepository<SubscriptionPlan,String>{Optional<SubscriptionPlan> findByMonthlyAmountPaiseAndActiveTrue(long amountPaise);}
