package com.pgconnect.admin;
import com.pgconnect.common.AuditLog;import com.pgconnect.management.Complaint;import com.pgconnect.management.PropertyReview;import com.pgconnect.subscription.OwnerSubscription;import com.pgconnect.subscription.SubscriptionInvoice;
import jakarta.validation.Valid;import jakarta.validation.constraints.Size;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController {
 private final AdminOperationsService service;public AdminOperationsController(AdminOperationsService service){this.service=service;}
 @GetMapping("/api/admin/overview") public AdminOperationsService.Overview overview(){return service.overview();}
 @GetMapping("/api/admin/owners") public List<AdminOperationsService.OwnerView> owners(){return service.owners();}
 @PatchMapping("/api/admin/owners/{id}/enabled") public AdminOperationsService.OwnerView ownerEnabled(@PathVariable String id,@RequestParam boolean enabled,@RequestParam(required=false) @Size(max=500) String reason,Authentication a){return service.setOwnerEnabled(a.getName(),id,enabled,reason);}
 @GetMapping("/api/admin/reports/listings") public List<AdminOperationsService.ReportView> reports(){return service.openReports();}
 @PostMapping("/api/admin/reports/listings/{id}/decision") public AdminOperationsService.ReportView reportDecision(@PathVariable String id,@Valid @RequestBody AdminOperationsService.ReportDecision r,Authentication a){return service.decideReport(a.getName(),id,r);}
 @PostMapping("/api/resident/properties/{propertyId}/report") @PreAuthorize("hasRole('RESIDENT')") public PropertyReport reportProperty(@PathVariable String propertyId,@Valid @RequestBody AdminOperationsService.ReportRequest r,Authentication a){return service.createPropertyReport(a.getName(),propertyId,r);}
 @GetMapping("/api/admin/reviews/reported") public List<PropertyReview> reportedReviews(){return service.reportedReviews();}
 @PostMapping("/api/admin/reviews/{id}/decision") public PropertyReview reviewDecision(@PathVariable String id,@Valid @RequestBody AdminOperationsService.ReviewDecision r,Authentication a){return service.moderateReview(a.getName(),id,r);}
 @GetMapping("/api/admin/complaints") public List<Complaint> complaints(){return service.complaints();}
 @GetMapping("/api/admin/subscriptions") public List<OwnerSubscription> subscriptions(){return service.subscriptions();}
 @GetMapping("/api/admin/subscription-invoices") public List<SubscriptionInvoice> invoices(){return service.invoices();}
 @GetMapping("/api/admin/audit-logs") public List<AuditLog> audit(@RequestParam(required=false) String entityType){return service.auditLogs(entityType);}
 @GetMapping("/api/admin/financial-report") public AdminOperationsService.FinancialReport financialReport(){return service.financialReport();}
}
