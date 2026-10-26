package com.pgconnect.common;
import org.springframework.data.mongodb.repository.MongoRepository;import java.util.List;
public interface AuditLogRepository extends MongoRepository<AuditLog,String>{List<AuditLog> findTop200ByOrderByCreatedAtDesc();List<AuditLog> findTop200ByEntityTypeOrderByCreatedAtDesc(String entityType);}
