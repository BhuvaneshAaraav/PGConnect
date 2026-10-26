package com.pgconnect.subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface RazorpayEventRepository extends MongoRepository<RazorpayEvent,String> {}
