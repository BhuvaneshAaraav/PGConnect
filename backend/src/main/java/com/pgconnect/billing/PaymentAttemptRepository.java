package com.pgconnect.billing;
import org.springframework.data.mongodb.repository.MongoRepository;import java.util.Optional;
public interface PaymentAttemptRepository extends MongoRepository<PaymentAttempt,String>{Optional<PaymentAttempt> findByIdempotencyKey(String key);Optional<PaymentAttempt> findByProviderOrderId(String orderId);Optional<PaymentAttempt> findByProviderPaymentId(String paymentId);}
