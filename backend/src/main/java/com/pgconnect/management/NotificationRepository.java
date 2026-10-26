package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface NotificationRepository extends MongoRepository<ResidentNotification,String>{ List<ResidentNotification> findTop100ByUserIdOrderByCreatedAtDesc(String userId); Optional<ResidentNotification> findByIdAndUserId(String id,String userId); boolean existsByUserIdAndTypeAndEntityId(String userId,String type,String entityId); }
