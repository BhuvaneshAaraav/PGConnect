package com.pgconnect.staff;

import com.pgconnect.management.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('STAFF')")
public class StaffOperationsController {
    private final StaffAccessService access;
    private final ManagementService management;

    public StaffOperationsController(StaffAccessService access, ManagementService management) {
        this.access = access;
        this.management = management;
    }

    @GetMapping("/api/staff/properties")
    public List<StaffAccessService.StaffPropertyView> properties(Authentication auth) {
        return access.staffProperties(auth.getName());
    }

    @GetMapping("/api/staff/properties/{propertyId}/complaints")
    public List<Complaint> complaints(@PathVariable String propertyId, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.COMPLAINTS);
        return management.ownerComplaints(ownerId, propertyId);
    }

    @PatchMapping("/api/staff/properties/{propertyId}/complaints/{complaintId}")
    public Complaint updateComplaint(@PathVariable String propertyId, @PathVariable String complaintId,
            @Valid @RequestBody ManagementController.ComplaintUpdate request, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.COMPLAINTS);
        return management.updateComplaint(auth.getName(), ownerId, propertyId, complaintId, request);
    }

    @GetMapping("/api/staff/properties/{propertyId}/announcements")
    public List<Announcement> announcements(@PathVariable String propertyId, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.ANNOUNCEMENTS);
        return management.ownerAnnouncements(ownerId, propertyId);
    }

    @PostMapping("/api/staff/properties/{propertyId}/announcements")
    public Announcement createAnnouncement(@PathVariable String propertyId,
            @Valid @RequestBody ManagementController.AnnouncementRequest request, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.ANNOUNCEMENTS);
        return management.createAnnouncement(auth.getName(), ownerId, propertyId, request);
    }

    @DeleteMapping("/api/staff/properties/{propertyId}/announcements/{id}")
    public void deleteAnnouncement(@PathVariable String propertyId, @PathVariable String id, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.ANNOUNCEMENTS);
        management.deleteAnnouncement(auth.getName(), ownerId, propertyId, id);
    }

    @GetMapping("/api/staff/properties/{propertyId}/food-menu")
    public List<ManagementController.FoodMenuView> menu(@PathVariable String propertyId, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.FOOD_MENU);
        return management.ownerMenu(ownerId, propertyId);
    }

    @PutMapping("/api/staff/properties/{propertyId}/food-menu")
    public ManagementController.FoodMenuView saveMenu(@PathVariable String propertyId,
            @Valid @RequestBody ManagementController.MenuRequest request, Authentication auth) {
        String ownerId = access.requireStaffOwner(auth.getName(), propertyId, StaffPermission.FOOD_MENU);
        return management.saveMenu(auth.getName(), ownerId, propertyId, request);
    }
}
