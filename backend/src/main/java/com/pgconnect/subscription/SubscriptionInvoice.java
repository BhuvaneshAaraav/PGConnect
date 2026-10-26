package com.pgconnect.subscription;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("subscription_invoices")
@CompoundIndex(name="provider_invoice_unique",def="{'providerInvoiceId':1}",unique=true,sparse=true)
public class SubscriptionInvoice {
    @Id private String id; private String ownerId; private String subscriptionId; private String providerInvoiceId;
    private String providerPaymentId; private long amountPaise; private String currency="INR"; private String status;
    private Instant createdAt=Instant.now();
    public String getId(){return id;} public String getOwnerId(){return ownerId;} public String getSubscriptionId(){return subscriptionId;}
    public String getProviderInvoiceId(){return providerInvoiceId;} public String getProviderPaymentId(){return providerPaymentId;}
    public long getAmountPaise(){return amountPaise;} public String getCurrency(){return currency;} public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;} public void setOwnerId(String v){ownerId=v;} public void setSubscriptionId(String v){subscriptionId=v;}
    public void setProviderInvoiceId(String v){providerInvoiceId=v;} public void setProviderPaymentId(String v){providerPaymentId=v;}
    public void setAmountPaise(long v){amountPaise=v;} public void setCurrency(String v){currency=v;} public void setStatus(String v){status=v;}
}
