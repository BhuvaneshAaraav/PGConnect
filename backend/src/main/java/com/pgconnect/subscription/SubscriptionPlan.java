package com.pgconnect.subscription;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.index.Indexed;import org.springframework.data.mongodb.core.mapping.Document;import java.time.Instant;
@Document("subscription_plans")
public class SubscriptionPlan {
 @Id private String id;@Indexed(unique=true)private long monthlyAmountPaise;private String providerPlanId;private boolean active=true;private Instant createdAt=Instant.now();
 public String getId(){return id;}public long getMonthlyAmountPaise(){return monthlyAmountPaise;}public String getProviderPlanId(){return providerPlanId;}public boolean isActive(){return active;}public Instant getCreatedAt(){return createdAt;}
 public void setMonthlyAmountPaise(long v){monthlyAmountPaise=v;}public void setProviderPlanId(String v){providerPlanId=v;}public void setActive(boolean v){active=v;}
}
