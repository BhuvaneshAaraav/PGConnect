package com.pgconnect.staff;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StaffController {
    private final StaffAccessService access;

    public StaffController(StaffAccessService access) { this.access = access; }

    @GetMapping("/api/owner/properties/{propertyId}/staff")
    @PreAuthorize("hasRole('OWNER')")
    public List<StaffAccessService.StaffMemberView> members(@PathVariable String propertyId, Authentication auth) {
        return access.ownerMembers(auth.getName(), propertyId);
    }

    @PostMapping("/api/owner/properties/{propertyId}/staff")
    @PreAuthorize("hasRole('OWNER')")
    public StaffAccessService.StaffMemberView add(@PathVariable String propertyId,
            @Valid @RequestBody StaffAccessService.AddMemberRequest request, Authentication auth) {
        return access.addMember(auth.getName(), propertyId, request);
    }

    @PatchMapping("/api/owner/properties/{propertyId}/staff/{membershipId}")
    @PreAuthorize("hasRole('OWNER')")
    public StaffAccessService.StaffMemberView update(@PathVariable String propertyId, @PathVariable String membershipId,
            @Valid @RequestBody StaffAccessService.UpdateMemberRequest request, Authentication auth) {
        return access.updateMember(auth.getName(), propertyId, membershipId, request);
    }

    @DeleteMapping("/api/owner/properties/{propertyId}/staff/{membershipId}")
    @PreAuthorize("hasRole('OWNER')")
    public void revoke(@PathVariable String propertyId, @PathVariable String membershipId, Authentication auth) {
        access.revokeMember(auth.getName(), propertyId, membershipId);
    }
}
