package com.pgconnect.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.common.AuditLog;
import com.pgconnect.management.NotificationRepository;
import com.pgconnect.management.ResidentNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionService {
    private final OwnerSubscriptionRepository subscriptions; private final SubscriptionInvoiceRepository invoices; private final SubscriptionPlanRepository plans;
    private final RazorpayEventRepository events; private final UserAccountRepository users; private final org.springframework.data.mongodb.core.MongoTemplate mongo; private final NotificationRepository notifications;
    private final ObjectMapper mapper; private final String keyId; private final String keySecret; private final String webhookSecret; private final long amountPaise; private final int graceDays;
    private final RestClient razorpay=RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();

    public SubscriptionService(OwnerSubscriptionRepository subscriptions, SubscriptionInvoiceRepository invoices, SubscriptionPlanRepository plans, RazorpayEventRepository events,
                               UserAccountRepository users, org.springframework.data.mongodb.core.MongoTemplate mongo, NotificationRepository notifications, ObjectMapper mapper,
                               @Value("${pgconnect.razorpay.key-id:}") String keyId,@Value("${pgconnect.razorpay.key-secret:}") String keySecret,
                               @Value("${pgconnect.razorpay.webhook-secret:}") String webhookSecret,
                               @Value("${pgconnect.subscription.monthly-amount-rupees:1000}") long amountRupees,
                               @Value("${pgconnect.subscription.grace-days:3}") int graceDays) {
        this.subscriptions=subscriptions;this.invoices=invoices;this.plans=plans;this.events=events;this.users=users;this.mongo=mongo;this.notifications=notifications;this.mapper=mapper;
        this.keyId=keyId;this.keySecret=keySecret;this.webhookSecret=webhookSecret;this.amountPaise=Math.multiplyExact(amountRupees,100L);this.graceDays=graceDays;
    }

    public Checkout start(String ownerId) {
        UserAccount owner=users.findById(ownerId).filter(u->u.getRole()==Role.OWNER).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(!owner.isEmailVerified())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify the owner email before starting a platform subscription");
        requireGateway();
        try {
            OwnerSubscription previous=subscriptions.findFirstByOwnerIdOrderByCreatedAtDesc(ownerId).orElse(null);
            if(previous!=null&&previous.getStatus()==OwnerSubscriptionStatus.PENDING&&previous.getProviderSubscriptionId()!=null)
                return new Checkout(keyId,previous.getProviderSubscriptionId(),previous.getMonthlyAmountPaise(),"INR",null,"Razorpay Subscription Checkout");
            if(previous!=null&&previous.getStatus()==OwnerSubscriptionStatus.ACTIVE&&previous.getCurrentPeriodEnd()!=null&&previous.getCurrentPeriodEnd().isAfter(Instant.now()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Your subscription is active through "+previous.getCurrentPeriodEnd()+"; it renews automatically.");
            String planId=planIdForConfiguredPrice();
            JsonNode created=razorpay.post().uri("/subscriptions").headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of(
                    "plan_id",planId,"total_count",120,"quantity",1,"customer_notify",1,"notes",Map.of("owner_id",ownerId))).retrieve().body(JsonNode.class);
            OwnerSubscription record=new OwnerSubscription();record.setOwnerId(ownerId);record.setProviderPlanId(planId);
            record.setProviderSubscriptionId(created.path("id").asText());record.setMonthlyAmountPaise(amountPaise);record.setStatus(OwnerSubscriptionStatus.PENDING);
            subscriptions.save(record);return new Checkout(keyId,record.getProviderSubscriptionId(),amountPaise,"INR",created.path("short_url").asText(null),"Razorpay Subscription Checkout");
        } catch (ResponseStatusException exception) { throw exception; } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Could not create the subscription with Razorpay. No subscription payment was recorded.");
        }
    }

    private String planIdForConfiguredPrice() throws Exception {
        SubscriptionPlan existing=plans.findByMonthlyAmountPaiseAndActiveTrue(amountPaise).orElse(null);
        if(existing!=null)return existing.getProviderPlanId();
        JsonNode remote=razorpay.post().uri("/plans").headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of("period","monthly","interval",1,
                "item",Map.of("name","PGConnect Owner Platform","amount",amountPaise,"currency","INR","description","Monthly owner management platform subscription"))).retrieve().body(JsonNode.class);
        SubscriptionPlan created=new SubscriptionPlan();created.setMonthlyAmountPaise(amountPaise);created.setProviderPlanId(remote.path("id").asText());
        try{return plans.save(created).getProviderPlanId();}catch(DuplicateKeyException race){return plans.findByMonthlyAmountPaiseAndActiveTrue(amountPaise).orElseThrow().getProviderPlanId();}
    }

    public OwnerSubscription current(String ownerId) {
        return subscriptions.findFirstByOwnerIdOrderByCreatedAtDesc(ownerId).orElse(null);
    }
    public List<SubscriptionInvoice> invoices(String ownerId){ return invoices.findByOwnerIdOrderByCreatedAtDesc(ownerId); }

    @org.springframework.transaction.annotation.Transactional
    public CancellationResult cancelAtCycleEnd(String ownerId) {
        OwnerSubscription subscription=subscriptions.findFirstByOwnerIdOrderByCreatedAtDesc(ownerId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
        if(subscription.getStatus()!=OwnerSubscriptionStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only an active subscription can have auto-renewal cancelled");
        if(subscription.isCancelAtCycleEnd())
            return new CancellationResult(subscription.getStatus(),true,subscription.getCurrentPeriodEnd(),"Auto-renewal is already scheduled to stop at the end of this billing period.");
        if(subscription.getProviderSubscriptionId()==null||subscription.getCurrentPeriodEnd()==null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"The active provider subscription is missing its renewal details");
        requireGateway();
        try {
            JsonNode response=razorpay.post().uri("/subscriptions/{id}/cancel",subscription.getProviderSubscriptionId())
                    .headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of("cancel_at_cycle_end",true)).retrieve().body(JsonNode.class);
            if(response==null||!subscription.getProviderSubscriptionId().equals(response.path("id").asText()))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay returned an unexpected subscription cancellation response");
            subscription.setCancelAtCycleEnd(true);subscription.setUpdatedAt(Instant.now());subscriptions.save(subscription);
            mongo.save(new AuditLog(ownerId,"SUBSCRIPTION_CANCELLATION_SCHEDULED","OwnerSubscription",subscription.getId(),
                    Map.of("providerSubscriptionId",subscription.getProviderSubscriptionId(),"cancelAt",subscription.getCurrentPeriodEnd().toString())));
            return new CancellationResult(subscription.getStatus(),true,subscription.getCurrentPeriodEnd(),"Auto-renewal will stop at the end of the current billing period. Access remains active until then.");
        } catch(ResponseStatusException exception){throw exception;} catch(Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay could not schedule subscription cancellation. No local subscription state was changed.");
        }
    }

    public CheckoutResult verifyCheckout(String ownerId, CheckoutResultRequest request) {
        OwnerSubscription subscription=subscriptions.findByProviderSubscriptionId(request.subscriptionId())
                .filter(s->s.getOwnerId().equals(ownerId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found"));
        String expected=hmac(keySecret,request.subscriptionId()+"|"+request.paymentId());
        if(!constantTimeEquals(expected,request.signature())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Razorpay signature could not be verified");
        // A signed browser callback proves callback integrity only. The subscription is activated by Razorpay's signed webhook event.
        return new CheckoutResult(subscription.getStatus(),"Payment response verified. Subscription access changes after the signed Razorpay webhook is confirmed.");
    }

    @org.springframework.transaction.annotation.Transactional
    public void handleWebhook(String rawBody,String signature,String eventId) {
        if(webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay webhook verification is not configured");
        String expected=hmac(webhookSecret,rawBody);
        if(!constantTimeEquals(expected,signature)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Webhook signature is invalid");
        try {
            JsonNode root=mapper.readTree(rawBody); String event=root.path("event").asText();
            String stableEventId=eventId==null||eventId.isBlank()?expected:eventId;
            RazorpayEvent seen=new RazorpayEvent();seen.setEventId(stableEventId);seen.setEvent(event);
            try{events.save(seen);}catch(DuplicateKeyException duplicate){return;}
            JsonNode entity=root.path("payload").path("subscription").path("entity");
            if(entity.isMissingNode()) entity=root.path("payload").path("subscription");
            String providerId=entity.path("id").asText("");
            if(providerId.isBlank()) return;
            OwnerSubscription subscription=subscriptions.findByProviderSubscriptionId(providerId).orElse(null);
            if(subscription==null) return;
            switch(event){
                case "subscription.activated", "subscription.charged" -> {subscription.setStatus(OwnerSubscriptionStatus.ACTIVE);subscription.setStartedAt(instantOrNow(entity.path("start_at")));
                    subscription.setCurrentPeriodStart(instant(entity.path("current_start")));subscription.setCurrentPeriodEnd(instant(entity.path("current_end")));
                    subscription.setRenewalAt(subscription.getCurrentPeriodEnd());updateOwner(subscription,SubscriptionStatus.ACTIVE);saveInvoice(root,subscription,event);}
                case "subscription.pending" -> {subscription.setStatus(OwnerSubscriptionStatus.PAST_DUE);updateOwner(subscription,SubscriptionStatus.PAST_DUE);}
                case "subscription.halted" -> {subscription.setStatus(OwnerSubscriptionStatus.SUSPENDED);updateOwner(subscription,SubscriptionStatus.SUSPENDED);}
                case "subscription.cancelled", "subscription.completed" -> {subscription.setStatus(OwnerSubscriptionStatus.CANCELLED);subscription.setCancelledAt(Instant.now());updateOwner(subscription,SubscriptionStatus.CANCELLED);
                    notifyOwnerOnce(subscription,"SUBSCRIPTION_CANCELLED","Owner plan cancelled","Your owner plan has ended. Historical records remain available.");}
                default -> {return;}
            }
            subscription.setUpdatedAt(Instant.now());subscriptions.save(subscription);
            mongo.save(new AuditLog(subscription.getOwnerId(),"RAZORPAY_"+event,"OwnerSubscription",subscription.getId(),Map.of("providerSubscriptionId",providerId,"eventId",stableEventId)));
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Webhook payload could not be processed");}
    }

    @org.springframework.scheduling.annotation.Scheduled(cron="0 25 * * * *",zone="Asia/Kolkata")
    @org.springframework.transaction.annotation.Transactional
    public void expireSubscriptions(){
        Instant now=Instant.now();
        Instant reminderWindowEnd=now.plusSeconds(3L*86400L);
        for(OwnerSubscription subscription:subscriptions.findByStatusAndCancelAtCycleEndFalseAndCurrentPeriodEndBetween(OwnerSubscriptionStatus.ACTIVE,now,reminderWindowEnd)){
            notifyOwnerOnce(subscription,"SUBSCRIPTION_RENEWAL_REMINDER","Subscription renewal is coming up",
                    "Your PGConnect owner plan is due to renew on "+subscription.getCurrentPeriodEnd()+". Manage renewal from your owner subscription page.");
        }
        for(OwnerSubscription subscription:subscriptions.findByCancelAtCycleEndTrueAndStatusInAndCurrentPeriodEndBefore(List.of(OwnerSubscriptionStatus.ACTIVE,OwnerSubscriptionStatus.PAST_DUE,OwnerSubscriptionStatus.SUSPENDED),now)){
            subscription.setStatus(OwnerSubscriptionStatus.CANCELLED);subscription.setCancelledAt(subscription.getCurrentPeriodEnd());subscription.setUpdatedAt(now);subscriptions.save(subscription);updateOwner(subscription,SubscriptionStatus.CANCELLED);
            notifyOwnerOnce(subscription,"SUBSCRIPTION_CANCELLED","Owner plan cancelled","Your owner plan ended at the close of the current billing period. Historical records remain available.");
            mongo.save(new AuditLog(subscription.getOwnerId(),"SUBSCRIPTION_CANCELLATION_EFFECTIVE","OwnerSubscription",subscription.getId(),Map.of("providerSubscriptionId",subscription.getProviderSubscriptionId())));
        }
        Instant expiredBefore=now.minusSeconds(Math.max(0,graceDays)*86400L);
        for(OwnerSubscription subscription:subscriptions.findByStatusInAndCurrentPeriodEndBefore(List.of(OwnerSubscriptionStatus.ACTIVE,OwnerSubscriptionStatus.PAST_DUE),expiredBefore)){
            subscription.setStatus(OwnerSubscriptionStatus.EXPIRED);subscription.setUpdatedAt(Instant.now());subscriptions.save(subscription);updateOwner(subscription,SubscriptionStatus.EXPIRED);
            notifyOwnerOnce(subscription,"SUBSCRIPTION_EXPIRED","Owner plan expired","Your PGConnect owner plan has expired. Renew it to restore listing eligibility.");
        }
    }
    private void notifyOwnerOnce(OwnerSubscription subscription,String type,String title,String message){
        String periodEnd=subscription.getCurrentPeriodEnd()==null?"unknown":subscription.getCurrentPeriodEnd().toString();
        String entityId=subscription.getId()+":"+periodEnd;
        if(notifications.existsByUserIdAndTypeAndEntityId(subscription.getOwnerId(),type,entityId))return;
        ResidentNotification notification=new ResidentNotification();notification.setUserId(subscription.getOwnerId());notification.setType(type);
        notification.setTitle(title);notification.setMessage(message);notification.setEntityType("OwnerSubscription");notification.setEntityId(entityId);notifications.save(notification);
    }
    private void saveInvoice(JsonNode root,OwnerSubscription sub,String event){
        JsonNode payment=root.path("payload").path("payment").path("entity");JsonNode invoice=root.path("payload").path("invoice").path("entity");
        String invoiceId=invoice.path("id").asText("");if(invoiceId.isBlank())return;
        SubscriptionInvoice record=new SubscriptionInvoice();record.setOwnerId(sub.getOwnerId());record.setSubscriptionId(sub.getId());record.setProviderInvoiceId(invoiceId);
        record.setProviderPaymentId(payment.path("id").asText(null));record.setAmountPaise(payment.path("amount").asLong(sub.getMonthlyAmountPaise()));record.setStatus(event);
        try{invoices.save(record);}catch(DuplicateKeyException ignored){ }
    }
    private void updateOwner(OwnerSubscription sub,SubscriptionStatus status){users.findById(sub.getOwnerId()).ifPresent(owner->{owner.setSubscriptionStatus(status);users.save(owner);});}
    private void requireGateway(){if(keyId.isBlank()||keySecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay is not configured. Set the server-side Razorpay credentials to start a subscription.");}
    private static Instant instant(JsonNode n){return n.isNumber()&&n.asLong()>0?Instant.ofEpochSecond(n.asLong()):null;}
    private static Instant instantOrNow(JsonNode n){Instant i=instant(n);return i==null?Instant.now():i;}
    private static String hmac(String secret,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean constantTimeEquals(String left,String right){if(left==null||right==null)return false;return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));}
    public record Checkout(String keyId,String subscriptionId,long amountPaise,String currency,String shortUrl,String name){}
    public record CheckoutResultRequest(String subscriptionId,String paymentId,String signature){}
    public record CheckoutResult(OwnerSubscriptionStatus status,String message){}
    public record CancellationResult(OwnerSubscriptionStatus status,boolean cancelAtCycleEnd,Instant currentPeriodEnd,String message){}
}
