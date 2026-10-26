package com.pgconnect.admin;

import com.pgconnect.auth.Role;import com.pgconnect.auth.UserAccount;import com.pgconnect.auth.UserAccountRepository;import com.pgconnect.billing.RentCharge;
import com.pgconnect.common.AuditLog;import com.pgconnect.common.AuditLogRepository;import com.pgconnect.management.Complaint;import com.pgconnect.management.ComplaintRepository;
import com.pgconnect.management.PropertyReview;import com.pgconnect.management.ReviewRepository;import com.pgconnect.property.PgProperty;import com.pgconnect.property.PgPropertyRepository;
import com.pgconnect.subscription.OwnerSubscription;import com.pgconnect.subscription.OwnerSubscriptionRepository;import com.pgconnect.subscription.SubscriptionInvoice;
import com.pgconnect.subscription.SubscriptionInvoiceRepository;import com.pgconnect.property.PublicListingPolicy;import org.springframework.data.mongodb.core.MongoTemplate;import org.springframework.data.mongodb.core.query.Criteria;import org.springframework.data.mongodb.core.query.Query;
import com.pgconnect.support.SupportTicketRepository;import com.pgconnect.support.SupportTicketStatus;
import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.web.server.ResponseStatusException;import java.time.Instant;import java.util.List;import java.util.Map;

@Service
public class AdminOperationsService {
 private final UserAccountRepository users;private final PgPropertyRepository properties;private final PropertyReportRepository reports;private final ReviewRepository reviews;private final ComplaintRepository complaints;
 private final OwnerSubscriptionRepository subscriptions;private final SubscriptionInvoiceRepository invoices;private final AuditLogRepository audit;private final MongoTemplate mongo;private final SupportTicketRepository supportTickets;
 public AdminOperationsService(UserAccountRepository users,PgPropertyRepository properties,PropertyReportRepository reports,ReviewRepository reviews,ComplaintRepository complaints,
  OwnerSubscriptionRepository subscriptions,SubscriptionInvoiceRepository invoices,AuditLogRepository audit,MongoTemplate mongo,SupportTicketRepository supportTickets){this.users=users;this.properties=properties;this.reports=reports;this.reviews=reviews;this.complaints=complaints;this.subscriptions=subscriptions;this.invoices=invoices;this.audit=audit;this.mongo=mongo;this.supportTickets=supportTickets;}
 public Overview overview(){
  long residents=users.countByRole(Role.RESIDENT),owners=users.countByRole(Role.OWNER),admins=users.countByRole(Role.ADMIN);
  long pendingProperties=properties.countByStatus(com.pgconnect.property.PropertyStatus.PENDING_REVIEW),published=properties.countByStatus(com.pgconnect.property.PropertyStatus.PUBLISHED);
  long activeSubscriptions=users.countByRoleAndSubscriptionStatus(Role.OWNER,com.pgconnect.auth.SubscriptionStatus.ACTIVE);
  long openReports=reports.countByStatus("OPEN"),reportedReviews=reviews.countByStatus("REPORTED"),openComplaints=complaints.countByStatusIn(List.of(com.pgconnect.management.ComplaintStatus.OPEN,com.pgconnect.management.ComplaintStatus.ASSIGNED,com.pgconnect.management.ComplaintStatus.IN_PROGRESS));
  List<SubscriptionInvoice> paid=invoices.findAll().stream().filter(i->"subscription.charged".equals(i.getStatus())||"paid".equalsIgnoreCase(i.getStatus())).toList();long revenue=paid.stream().mapToLong(SubscriptionInvoice::getAmountPaise).sum();
  long openSupport=supportTickets.countByStatusIn(List.of(SupportTicketStatus.OPEN,SupportTicketStatus.IN_PROGRESS));
  return new Overview(residents,owners,admins,pendingProperties,published,activeSubscriptions,openReports,reportedReviews,openComplaints,openSupport,revenue);
 }
 public List<OwnerView> owners(){return users.findAll().stream().filter(u->u.getRole()==Role.OWNER).limit(500).map(u->{OwnerSubscription s=subscriptions.findFirstByOwnerIdOrderByCreatedAtDesc(u.getId()).orElse(null);
   return new OwnerView(u.getId(),u.getName(),u.getEmailNormalized(),u.isEnabled(),u.getSubscriptionStatus()==null?null:u.getSubscriptionStatus().name(),s==null?null:s.getCurrentPeriodEnd(),properties.findByOwnerIdOrderByCreatedAtDesc(u.getId()).size());}).toList();}
 public OwnerView setOwnerEnabled(String adminId,String ownerId,boolean enabled,String reason){
  UserAccount owner=users.findById(ownerId).filter(u->u.getRole()==Role.OWNER).orElseThrow(()->notFound("Owner not found"));owner.setEnabled(enabled);users.save(owner);
  mongo.save(new AuditLog(adminId,enabled?"OWNER_ENABLED":"OWNER_DISABLED","User",ownerId,Map.of("reason",reason==null?"":reason)));
  OwnerSubscription s=subscriptions.findFirstByOwnerIdOrderByCreatedAtDesc(ownerId).orElse(null);return new OwnerView(owner.getId(),owner.getName(),owner.getEmailNormalized(),owner.isEnabled(),owner.getSubscriptionStatus()==null?null:owner.getSubscriptionStatus().name(),s==null?null:s.getCurrentPeriodEnd(),properties.findByOwnerIdOrderByCreatedAtDesc(ownerId).size());
 }
 public PropertyReport createPropertyReport(String reporterId,String propertyId,ReportRequest request){
  PgProperty p=properties.findById(propertyId).filter(v->PublicListingPolicy.isEligible(v,users.findById(v.getOwnerId()).orElse(null))).orElseThrow(()->notFound("Listing not found"));
  PropertyReport r=new PropertyReport();r.setPropertyId(propertyId);r.setOwnerId(p.getOwnerId());r.setReporterId(reporterId);r.setReason(request.reason().trim());r.setDetails(request.details()==null?null:request.details().trim());return reports.save(r);
 }
 public List<ReportView> openReports(){return reports.findByStatusOrderByCreatedAtAsc("OPEN").stream().map(this::reportView).toList();}
 public ReportView decideReport(String adminId,String id,ReportDecision request){
  PropertyReport r=reports.findById(id).orElseThrow(()->notFound("Report not found"));if(!"OPEN".equals(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Report was already reviewed");
  r.setStatus(request.suspendListing()?"ACTIONED":"DISMISSED");r.setAdminResponse(request.response().trim());r.setReviewedBy(adminId);r.setReviewedAt(Instant.now());reports.save(r);
  if(request.suspendListing())properties.findById(r.getPropertyId()).ifPresent(p->{p.setStatus(com.pgconnect.property.PropertyStatus.SUSPENDED);p.setSuspended(true);p.setListingEnabled(false);p.setUpdatedAt(Instant.now());properties.save(p);});
  mongo.save(new AuditLog(adminId,"PROPERTY_REPORT_"+r.getStatus(),"PropertyReport",id,Map.of("propertyId",r.getPropertyId(),"suspendListing",Boolean.toString(request.suspendListing()))));return reportView(r);
 }
 public List<PropertyReview> reportedReviews(){return reviews.findByStatusOrderByCreatedAtAsc("REPORTED");}
 public PropertyReview moderateReview(String adminId,String id,ReviewDecision request){PropertyReview r=reviews.findById(id).orElseThrow(()->notFound("Review not found"));if(!List.of("REPORTED","PUBLISHED").contains(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Review is no longer available for moderation");r.setStatus(request.publish()?"PUBLISHED":"REMOVED");r.setUpdatedAt(Instant.now());reviews.save(r);mongo.save(new AuditLog(adminId,"REVIEW_MODERATED","Review",id,Map.of("status",r.getStatus(),"reason",request.reason()==null?"":request.reason())));return r;}
 public List<Complaint> complaints(){return complaints.findAll().stream().sorted((x,y)->y.getCreatedAt().compareTo(x.getCreatedAt())).limit(500).toList();}
 public List<OwnerSubscription> subscriptions(){return subscriptions.findAll().stream().sorted((x,y)->y.getCreatedAt().compareTo(x.getCreatedAt())).limit(500).toList();}
 public List<SubscriptionInvoice> invoices(){return invoices.findAll().stream().sorted((x,y)->y.getCreatedAt().compareTo(x.getCreatedAt())).limit(1000).toList();}
 public List<AuditLog> auditLogs(String entityType){return entityType==null||entityType.isBlank()?audit.findTop200ByOrderByCreatedAtDesc():audit.findTop200ByEntityTypeOrderByCreatedAtDesc(entityType);}
 public FinancialReport financialReport(){List<SubscriptionInvoice> all=invoices.findAll();long total=all.stream().filter(i->"subscription.charged".equals(i.getStatus())||"paid".equalsIgnoreCase(i.getStatus())).mapToLong(SubscriptionInvoice::getAmountPaise).sum();long captured=all.stream().filter(i->"subscription.charged".equals(i.getStatus())||"paid".equalsIgnoreCase(i.getStatus())).count();
  List<RentCharge> rent=mongo.find(Query.query(Criteria.where("outstandingBalance").gt(0)),RentCharge.class);long outstanding=rent.stream().mapToLong(RentCharge::getOutstandingBalance).sum();return new FinancialReport(total,captured,all.size(),outstanding,"Subscription revenue is shown separately from residents' rent outstanding.");}
 private ReportView reportView(PropertyReport r){PgProperty p=properties.findById(r.getPropertyId()).orElse(null);return new ReportView(r.getId(),r.getPropertyId(),p==null?"Property removed":p.getName(),r.getOwnerId(),r.getReporterId(),r.getReason(),r.getDetails(),r.getStatus(),r.getCreatedAt());}
 private static ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
 public record Overview(long residents,long owners,long admins,long propertiesAwaitingReview,long publishedProperties,long activeSubscriptions,long openListingReports,long reportedReviews,long openComplaints,long openSupportRequests,long subscriptionRevenuePaise){}
 public record OwnerView(String id,String name,String email,boolean enabled,String subscriptionStatus,Instant currentPeriodEnd,int propertyCount){}
 public record ReportRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100) String reason,@jakarta.validation.constraints.Size(max=1000) String details){}
 public record ReportDecision(boolean suspendListing,@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=1000) String response){}
 public record ReviewDecision(boolean publish,@jakarta.validation.constraints.Size(max=500) String reason){}
 public record ReportView(String id,String propertyId,String propertyName,String ownerId,String reporterId,String reason,String details,String status,Instant createdAt){}
 public record FinancialReport(long subscriptionRevenuePaise,long paidSubscriptionInvoices,long invoiceCount,long rentOutstandingPaise,String separationNote){}
}
