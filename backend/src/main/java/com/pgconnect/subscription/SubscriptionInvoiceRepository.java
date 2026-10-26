package com.pgconnect.subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface SubscriptionInvoiceRepository extends MongoRepository<SubscriptionInvoice,String> { List<SubscriptionInvoice> findByOwnerIdOrderByCreatedAtDesc(String ownerId); }
