package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface AnnouncementRepository extends MongoRepository<Announcement,String> {
    List<Announcement> findByPropertyIdAndPublishedTrueOrderByPublishedAtDesc(String propertyId);
    List<Announcement> findByOwnerIdAndPropertyIdOrderByPublishedAtDesc(String ownerId,String propertyId);
}
