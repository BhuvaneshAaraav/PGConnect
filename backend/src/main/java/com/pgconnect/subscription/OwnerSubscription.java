package com.pgconnect.subscription;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("owner_subscriptions")
@CompoundIndex(name = "owner_subscription_history", def = "{'ownerId': 1, 'createdAt': -1}")
public class OwnerSubscription {
    @Id private String id;
    @Indexed private String ownerId;
    private String providerPlanId;
    @Indexed(unique = true, sparse = true) private String providerSubscriptionId;
    private long monthlyAmountPaise;
    private OwnerSubscriptionStatus status = OwnerSubscriptionStatus.PENDING;
    private Instant startedAt;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant renewalAt;
    private Instant cancelledAt;
    private boolean cancelAtCycleEnd;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    public String getId(){return id;} public String getOwnerId(){return ownerId;} public String getProviderPlanId(){return providerPlanId;}
    public String getProviderSubscriptionId(){return providerSubscriptionId;} public long getMonthlyAmountPaise(){return monthlyAmountPaise;}
    public OwnerSubscriptionStatus getStatus(){return status;} public Instant getStartedAt(){return startedAt;}
    public Instant getCurrentPeriodStart(){return currentPeriodStart;} public Instant getCurrentPeriodEnd(){return currentPeriodEnd;}
    public Instant getRenewalAt(){return renewalAt;} public Instant getCancelledAt(){return cancelledAt;}
    public boolean isCancelAtCycleEnd(){return cancelAtCycleEnd;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void setOwnerId(String v){ownerId=v;} public void setProviderPlanId(String v){providerPlanId=v;}
    public void setProviderSubscriptionId(String v){providerSubscriptionId=v;} public void setMonthlyAmountPaise(long v){monthlyAmountPaise=v;}
    public void setStatus(OwnerSubscriptionStatus v){status=v;} public void setStartedAt(Instant v){startedAt=v;}
    public void setCurrentPeriodStart(Instant v){currentPeriodStart=v;} public void setCurrentPeriodEnd(Instant v){currentPeriodEnd=v;}
    public void setRenewalAt(Instant v){renewalAt=v;} public void setCancelledAt(Instant v){cancelledAt=v;}
    public void setCancelAtCycleEnd(boolean v){cancelAtCycleEnd=v;}
    public void setUpdatedAt(Instant v){updatedAt=v;}
}
