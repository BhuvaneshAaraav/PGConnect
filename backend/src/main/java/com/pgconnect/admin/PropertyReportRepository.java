package com.pgconnect.admin;
import org.springframework.data.mongodb.repository.MongoRepository;import java.util.List;
public interface PropertyReportRepository extends MongoRepository<PropertyReport,String>{List<PropertyReport> findByStatusOrderByCreatedAtAsc(String status);long countByStatus(String status);}
