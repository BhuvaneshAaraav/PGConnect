package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface ReviewRepository extends MongoRepository<PropertyReview,String>{
    List<PropertyReview> findByPropertyIdAndStatusOrderByCreatedAtDesc(String propertyId,String status);
    Optional<PropertyReview> findByIdAndOwnerId(String id,String ownerId);
    List<PropertyReview> findByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
}
