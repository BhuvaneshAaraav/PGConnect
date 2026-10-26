package com.pgconnect.billing;

import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;import com.pgconnect.common.AuditLog;
import org.springframework.beans.factory.annotation.Value;import org.springframework.dao.DuplicateKeyException;import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;import org.springframework.data.mongodb.core.query.Query;import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.web.client.RestClient;import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.time.Instant;import java.util.HexFormat;import java.util.Map;import java.util.UUID;

@Service
public class RentGatewayService {
 private final String keyId,keySecret,webhookSecret;private final ObjectMapper mapper;private final RentChargeRepository charges;private final PaymentAttemptRepository attempts;private final RentPaymentRepository payments;private final MongoTemplate mongo;
 private final RestClient gateway=RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();
 public RentGatewayService(@Value("${pgconnect.razorpay.key-id:}") String keyId,@Value("${pgconnect.razorpay.key-secret:}") String keySecret,@Value("${pgconnect.razorpay.webhook-secret:}") String webhookSecret,
  ObjectMapper mapper,RentChargeRepository charges,PaymentAttemptRepository attempts,RentPaymentRepository payments,MongoTemplate mongo){this.keyId=keyId;this.keySecret=keySecret;this.webhookSecret=webhookSecret;this.mapper=mapper;this.charges=charges;this.attempts=attempts;this.payments=payments;this.mongo=mongo;}
 public Checkout start(String residentId,String chargeId,CheckoutRequest request){
  requireGateway();RentCharge charge=charges.findByIdAndResidentId(chargeId,residentId).orElseThrow(()->notFound("Rent charge not found"));
  if(charge.getOutstandingBalance()<=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"This rent charge is already paid");
  PaymentAttempt existing=attempts.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
  if(existing!=null){if(existing.getResidentId().equals(residentId)&&existing.getChargeId().equals(chargeId))return checkout(existing);throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment request key has already been used");}
  long amountPaise=Math.multiplyExact(charge.getOutstandingBalance(),100L);String receipt="r_"+UUID.randomUUID().toString().replace("-","").substring(0,24);
  try{JsonNode order=gateway.post().uri("/orders").headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of("amount",amountPaise,"currency","INR","receipt",receipt,"notes",Map.of("charge_id",chargeId,"resident_id",residentId))).retrieve().body(JsonNode.class);
   PaymentAttempt attempt=new PaymentAttempt();attempt.setResidentId(residentId);attempt.setOwnerId(charge.getOwnerId());attempt.setChargeId(chargeId);attempt.setPropertyId(charge.getPropertyId());attempt.setProviderOrderId(order.path("id").asText());attempt.setIdempotencyKey(request.idempotencyKey());attempt.setAmountPaise(amountPaise);attempt=attempts.save(attempt);return checkout(attempt);
  }catch(DuplicateKeyException e){return attempts.findByIdempotencyKey(request.idempotencyKey()).map(this::checkout).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Payment request already exists"));}
  catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Could not start rent payment with Razorpay. No rent payment was recorded.");}
 }
 @org.springframework.transaction.annotation.Transactional
 public VerifyResult verify(String residentId,VerifyRequest request){
  PaymentAttempt attempt=attempts.findByProviderOrderId(request.orderId()).orElseThrow(()->notFound("Payment attempt not found"));if(!attempt.getResidentId().equals(residentId))throw notFound("Payment attempt not found");
  verifySignature(request.orderId()+"|"+request.paymentId(),request.signature(),keySecret);
  JsonNode payment;try{payment=gateway.get().uri("/payments/{id}",request.paymentId()).headers(h->h.setBasicAuth(keyId,keySecret)).retrieve().body(JsonNode.class);}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay payment status is temporarily unavailable. The charge was not updated.");}
  if(!attempt.getProviderOrderId().equals(payment.path("order_id").asText())||payment.path("amount").asLong()!=attempt.getAmountPaise()||!"INR".equals(payment.path("currency").asText())||!"captured".equals(payment.path("status").asText()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Razorpay has not confirmed a captured payment matching this rent charge");
  confirm(attempt,request.paymentId(),payment.path("amount").asLong());return new VerifyResult("CONFIRMED","Razorpay confirmed the captured payment.");
 }
 @org.springframework.transaction.annotation.Transactional
 public void webhook(String raw,String signature,String eventId){
  if(webhookSecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay webhook verification is not configured");verifySignature(raw,signature,webhookSecret);
  try{JsonNode root=mapper.readTree(raw);String event=root.path("event").asText();JsonNode payment=root.path("payload").path("payment").path("entity");
   if(!"payment.captured".equals(event)&&!"order.paid".equals(event))return;
   String orderId=payment.path("order_id").asText("");String paymentId=payment.path("id").asText("");if(orderId.isBlank()||paymentId.isBlank())return;
   PaymentAttempt attempt=attempts.findByProviderOrderId(orderId).orElse(null);if(attempt==null)return;
   if(payment.path("amount").asLong()!=attempt.getAmountPaise()||!"INR".equals(payment.path("currency").asText()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Captured payment details do not match the rent attempt");
   confirm(attempt,paymentId,payment.path("amount").asLong());
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Razorpay rent webhook payload could not be processed");}
 }
 private void confirm(PaymentAttempt attempt,String paymentId,long amountPaise){
  PaymentAttempt latest=attempts.findByProviderOrderId(attempt.getProviderOrderId()).orElseThrow(()->notFound("Payment attempt not found"));
  if(latest.getStatus()==PaymentAttemptStatus.CONFIRMED){if(paymentId.equals(latest.getProviderPaymentId()))return;throw new ResponseStatusException(HttpStatus.CONFLICT,"This order was already confirmed with another payment");}
  Query claim=Query.query(Criteria.where("_id").is(latest.getId()).and("status").is(PaymentAttemptStatus.PENDING));
  PaymentAttempt claimed=mongo.findAndModify(claim,new Update().set("status",PaymentAttemptStatus.CONFIRMING),PaymentAttempt.class);
  if(claimed==null){PaymentAttempt current=attempts.findById(latest.getId()).orElseThrow(()->notFound("Payment attempt not found"));if(current.getStatus()==PaymentAttemptStatus.CONFIRMED&&paymentId.equals(current.getProviderPaymentId()))return;throw new ResponseStatusException(HttpStatus.CONFLICT,"This payment attempt is being reconciled. Refresh before retrying.");}
  if(amountPaise%100!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment amount cannot be allocated in whole rupees");long amount=amountPaise/100;
  RentCharge charge=charges.findById(latest.getChargeId()).orElseThrow(()->notFound("Rent charge not found"));if(amount>charge.getOutstandingBalance()){markFailed(claimed);throw new ResponseStatusException(HttpStatus.CONFLICT,"Captured amount exceeds the current charge balance. Reconciliation is required.");}
  String paymentKey="razorpay:"+paymentId;RentPayment rentPayment=payments.findByIdempotencyKey(paymentKey).orElse(null);
  if(rentPayment==null){rentPayment=new RentPayment();rentPayment.setOwnerId(latest.getOwnerId());rentPayment.setResidentId(latest.getResidentId());rentPayment.setTenancyId(charge.getTenancyId());rentPayment.setChargeId(latest.getChargeId());rentPayment.setPropertyId(latest.getPropertyId());rentPayment.setAmount(amount);rentPayment.setMethod(PaymentMethod.GATEWAY);rentPayment.setStatus(RentPaymentStatus.CONFIRMED);rentPayment.setRecordedBy("RAZORPAY");rentPayment.setGatewayPaymentId(paymentId);rentPayment.setGatewayVerified(true);rentPayment.setIdempotencyKey(paymentKey);rentPayment.setReference(latest.getProviderOrderId());rentPayment.setReceiptNumber("PG-"+UUID.randomUUID().toString().substring(0,12).toUpperCase());rentPayment.setConfirmedAt(Instant.now());rentPayment=payments.save(rentPayment);}
  RentChargeStatus nextStatus=amount==charge.getOutstandingBalance()?RentChargeStatus.PAID:RentChargeStatus.PARTIALLY_PAID;
  Query balance=Query.query(Criteria.where("_id").is(charge.getId()).and("outstandingBalance").gte(amount));RentCharge changed=mongo.findAndModify(balance,new Update().inc("paidAmount",amount).inc("outstandingBalance",-amount).set("status",nextStatus).set("updatedAt",Instant.now()),RentCharge.class);
  if(changed==null){if(rentPayment.getId()!=null)payments.deleteById(rentPayment.getId());markFailed(claimed);throw new ResponseStatusException(HttpStatus.CONFLICT,"Charge balance changed; reconciliation is required before retrying this payment");}
  mongo.save(new PaymentAllocation(rentPayment.getId(),charge.getId(),amount));Query mark=Query.query(Criteria.where("_id").is(latest.getId()).and("status").is(PaymentAttemptStatus.CONFIRMING));mongo.updateFirst(mark,new Update().set("status",PaymentAttemptStatus.CONFIRMED).set("providerPaymentId",paymentId).set("confirmedAt",Instant.now()),PaymentAttempt.class);
  mongo.save(new AuditLog("RAZORPAY","GATEWAY_RENT_PAYMENT_CONFIRMED","RentCharge",charge.getId(),Map.of("amount",Long.toString(amount),"paymentId",paymentId,"orderId",latest.getProviderOrderId())));
 }
 private void markFailed(PaymentAttempt attempt){mongo.updateFirst(Query.query(Criteria.where("_id").is(attempt.getId()).and("status").is(PaymentAttemptStatus.CONFIRMING)),new Update().set("status",PaymentAttemptStatus.FAILED),PaymentAttempt.class);}
 private Checkout checkout(PaymentAttempt a){return new Checkout(keyId,a.getProviderOrderId(),a.getAmountPaise(),a.getCurrency(),a.getChargeId());}
 private void requireGateway(){if(keyId.isBlank()||keySecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay is not configured. Set server-side credentials before starting online rent payments.");}
 private static void verifySignature(String payload,String signature,String secret){if(secret==null||secret.isBlank()||!constantTimeEquals(hmac(secret,payload),signature))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Razorpay signature is invalid");}
 private static String hmac(String key,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static boolean constantTimeEquals(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
 private static ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
 public record CheckoutRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100)String idempotencyKey){}
 public record VerifyRequest(@jakarta.validation.constraints.NotBlank String orderId,@jakarta.validation.constraints.NotBlank String paymentId,@jakarta.validation.constraints.NotBlank String signature){}
 public record Checkout(String keyId,String orderId,long amountPaise,String currency,String chargeId){}
 public record VerifyResult(String status,String message){}
}
