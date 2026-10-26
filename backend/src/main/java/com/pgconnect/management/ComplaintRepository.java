package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface ComplaintRepository extends MongoRepository<Complaint,String> {
    List<Complaint> findByResidentIdOrderByCreatedAtDesc(String residentId);
    List<Complaint> findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(String ownerId,String propertyId);
    Optional<Complaint> findByIdAndResidentId(String id,String residentId);
    Optional<Complaint> findByIdAndOwnerIdAndPropertyId(String id,String ownerId,String propertyId);
    long countByStatusIn(List<ComplaintStatus> statuses);
}
