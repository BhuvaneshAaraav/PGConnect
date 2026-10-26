package com.pgconnect.management;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.index.CompoundIndex;import org.springframework.data.mongodb.core.mapping.Document;
@Document("food_menus") @CompoundIndex(name="property_weekly_meal",def="{'propertyId':1,'dayOfWeek':1,'meal':1}",unique=true)
public class FoodMenu {
 @Id private String id;private String ownerId;private String propertyId;private int dayOfWeek;private String meal;private String description;private String vegetarianOption;private String nonVegetarianOption;private String mealTime;private boolean published;
 public String getId(){return id;}public String getOwnerId(){return ownerId;}public String getPropertyId(){return propertyId;}public int getDayOfWeek(){return dayOfWeek;}public String getMeal(){return meal;}public String getDescription(){return description;}public String getVegetarianOption(){return vegetarianOption;}public String getNonVegetarianOption(){return nonVegetarianOption;}public String getMealTime(){return mealTime;}public boolean isPublished(){return published;}
 public void setOwnerId(String v){ownerId=v;}public void setPropertyId(String v){propertyId=v;}public void setDayOfWeek(int v){dayOfWeek=v;}public void setMeal(String v){meal=v;}public void setDescription(String v){description=v;}public void setVegetarianOption(String v){vegetarianOption=v;}public void setNonVegetarianOption(String v){nonVegetarianOption=v;}public void setMealTime(String v){mealTime=v;}public void setPublished(boolean v){published=v;}
}
