package com.pgconnect.management;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
public class ManagementController {
    private final ManagementService service; public ManagementController(ManagementService service){this.service=service;}
    @GetMapping("/api/resident/complaints") @PreAuthorize("hasRole('RESIDENT')") public List<Complaint> residentComplaints(Authentication a){return service.residentComplaints(a.getName());}
    @PostMapping("/api/resident/complaints") @PreAuthorize("hasRole('RESIDENT')") public Complaint createComplaint(@Valid @RequestBody ComplaintRequest r,Authentication a){return service.createComplaint(a.getName(),r);}
    @GetMapping("/api/owner/properties/{propertyId}/complaints") @PreAuthorize("hasRole('OWNER')") public List<Complaint> ownerComplaints(@PathVariable String propertyId,Authentication a){return service.ownerComplaints(a.getName(),propertyId);}
    @PatchMapping("/api/owner/properties/{propertyId}/complaints/{complaintId}") @PreAuthorize("hasRole('OWNER')") public Complaint updateComplaint(@PathVariable String propertyId,@PathVariable String complaintId,@Valid @RequestBody ComplaintUpdate r,Authentication a){return service.updateComplaint(a.getName(),propertyId,complaintId,r);}
    @GetMapping("/api/owner/properties/{propertyId}/announcements") @PreAuthorize("hasRole('OWNER')") public List<Announcement> ownerAnnouncements(@PathVariable String propertyId,Authentication a){return service.ownerAnnouncements(a.getName(),propertyId);}
    @PostMapping("/api/owner/properties/{propertyId}/announcements") @PreAuthorize("hasRole('OWNER')") public Announcement createAnnouncement(@PathVariable String propertyId,@Valid @RequestBody AnnouncementRequest r,Authentication a){return service.createAnnouncement(a.getName(),propertyId,r);}
    @DeleteMapping("/api/owner/properties/{propertyId}/announcements/{id}") @PreAuthorize("hasRole('OWNER')") public void deleteAnnouncement(@PathVariable String propertyId,@PathVariable String id,Authentication a){service.deleteAnnouncement(a.getName(),propertyId,id);}
    @GetMapping("/api/resident/announcements") @PreAuthorize("hasRole('RESIDENT')") public List<Announcement> residentAnnouncements(Authentication a){return service.residentAnnouncements(a.getName());}
    @GetMapping("/api/resident/food-menu") @PreAuthorize("hasRole('RESIDENT')") public List<FoodMenuView> residentMenu(Authentication a){return service.residentMenu(a.getName());}
    @GetMapping("/api/owner/properties/{propertyId}/food-menu") @PreAuthorize("hasRole('OWNER')") public List<FoodMenuView> ownerMenu(@PathVariable String propertyId,Authentication a){return service.ownerMenu(a.getName(),propertyId);}
    @PutMapping("/api/owner/properties/{propertyId}/food-menu") @PreAuthorize("hasRole('OWNER')") public FoodMenuView saveMenu(@PathVariable String propertyId,@Valid @RequestBody MenuRequest r,Authentication a){return service.saveMenu(a.getName(),propertyId,r);}
    @GetMapping("/api/resident/notifications") @PreAuthorize("hasRole('RESIDENT')") public List<ResidentNotification> residentNotifications(Authentication a){return service.notifications(a.getName());}
    @GetMapping("/api/owner/notifications") @PreAuthorize("hasRole('OWNER')") public List<ResidentNotification> ownerNotifications(Authentication a){return service.notifications(a.getName());}
    @PostMapping("/api/notifications/{id}/read") public ResidentNotification markRead(@PathVariable String id,Authentication a){return service.markNotificationRead(a.getName(),id);}
    @GetMapping("/api/public/properties/{propertyId}/reviews") public ManagementService.ReviewSummary publicReviews(@PathVariable String propertyId){return service.publicReviews(propertyId);}
    @PostMapping("/api/resident/properties/{propertyId}/reviews") @PreAuthorize("hasRole('RESIDENT')") public PropertyReview createReview(@PathVariable String propertyId,@Valid @RequestBody ReviewRequest r,Authentication a){return service.createReview(a.getName(),propertyId,r);}
    @PostMapping("/api/resident/reviews/{id}/report") @PreAuthorize("hasRole('RESIDENT')") public PropertyReview reportReview(@PathVariable String id,@Valid @RequestBody ReviewReport r,Authentication a){return service.reportReview(a.getName(),id,r);}
    @GetMapping("/api/owner/properties/{propertyId}/deposits") @PreAuthorize("hasRole('OWNER')") public List<Deposit> deposits(@PathVariable String propertyId,Authentication a){return service.deposits(a.getName(),propertyId);}
    @PostMapping("/api/owner/properties/{propertyId}/deposits") @PreAuthorize("hasRole('OWNER')") public Deposit recordDeposit(@PathVariable String propertyId,@Valid @RequestBody DepositRequest r,Authentication a){return service.recordDeposit(a.getName(),propertyId,r);}
    @PostMapping("/api/owner/properties/{propertyId}/deposits/{id}/refund") @PreAuthorize("hasRole('OWNER')") public Deposit refundDeposit(@PathVariable String propertyId,@PathVariable String id,@Valid @RequestBody RefundRequest r,Authentication a){return service.refundDeposit(a.getName(),propertyId,id,r);}
    public record ComplaintRequest(@NotBlank @Size(max=50) String category,@NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String description){}
    public record ComplaintUpdate(@NotNull ComplaintStatus status,@Size(max=1000) String response,@Size(max=100) String assignedTo){}
    public record AnnouncementRequest(@NotBlank @Size(max=140) String title,@NotBlank @Size(max=2000) String body,String buildingId,String residentId,Instant expiresAt){}
    public record MenuRequest(@NotNull @Min(1) @Max(7) Integer dayOfWeek,@NotBlank @Size(max=30) String meal,@NotBlank @Size(max=500) String description,@Size(max=30) String vegetarianOption,@Size(max=30) String nonVegetarianOption,@Size(max=30) String mealTime,boolean published){}
    public record FoodMenuView(String id,String propertyId,int dayOfWeek,String meal,String description,String vegetarianOption,String nonVegetarianOption,String mealTime,boolean published){}
    public record ReviewRequest(@Min(1) @Max(5) int rating,@NotBlank @Size(min=10,max=2000) String text){}
    public record ReviewReport(@NotBlank @Size(max=500) String reason){}
    public record DepositRequest(@NotBlank String tenancyId,@Min(1) @Max(100000000) long amount,@Size(max=160) String reference){}
    public record RefundRequest(@Min(1) @Max(100000000) long amount,@NotBlank @Size(max=500) String reason){}
}
