package com.pgconnect.management;
import org.springframework.data.mongodb.repository.MongoRepository;import java.util.List;import java.util.Optional;
public interface FoodMenuRepository extends MongoRepository<FoodMenu,String>{List<FoodMenu> findByPropertyIdAndPublishedTrueOrderByDayOfWeekAsc(String propertyId);List<FoodMenu> findByOwnerIdAndPropertyIdOrderByDayOfWeekAsc(String ownerId,String propertyId);Optional<FoodMenu> findByOwnerIdAndPropertyIdAndDayOfWeekAndMeal(String ownerId,String propertyId,int dayOfWeek,String meal);}
