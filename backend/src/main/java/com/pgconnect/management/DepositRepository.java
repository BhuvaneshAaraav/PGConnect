package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface DepositRepository extends MongoRepository<Deposit,String>{ List<Deposit> findByOwnerIdAndPropertyIdOrderByReceivedAtDesc(String ownerId,String propertyId); List<Deposit> findByResidentIdOrderByReceivedAtDesc(String residentId); }
