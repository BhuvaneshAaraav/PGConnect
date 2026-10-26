package com.pgconnect.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PropertyLifecycleController {
    private final PropertyLifecycleService lifecycle;
    public PropertyLifecycleController(PropertyLifecycleService lifecycle) { this.lifecycle = lifecycle; }

    @PutMapping("/api/owner/properties/{propertyId}")
    @PreAuthorize("hasRole('OWNER')")
    public PgProperty update(@PathVariable String propertyId, @Valid @RequestBody PropertyService.CreatePropertyRequest request, Authentication auth) {
        return lifecycle.updateDraft(auth.getName(), propertyId, request);
    }

    @PostMapping("/api/owner/properties/{propertyId}/submit")
    @PreAuthorize("hasRole('OWNER')")
    public PgProperty submit(@PathVariable String propertyId, Authentication auth) { return lifecycle.submitForReview(auth.getName(), propertyId); }

    @PatchMapping("/api/owner/properties/{propertyId}/listing")
    @PreAuthorize("hasRole('OWNER')")
    public PgProperty listing(@PathVariable String propertyId, @RequestParam boolean enabled, Authentication auth) {
        return lifecycle.setListingEnabled(auth.getName(), propertyId, enabled);
    }

    @GetMapping("/api/admin/properties/review-queue")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PgProperty> queue() { return lifecycle.adminQueue(); }

    @PostMapping("/api/admin/properties/{propertyId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public PgProperty decision(@PathVariable String propertyId, @Valid @RequestBody PropertyLifecycleService.AdminPropertyDecision decision, Authentication auth) {
        return lifecycle.adminDecision(auth.getName(), propertyId, decision);
    }

    @PostMapping("/api/admin/properties/{propertyId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public PgProperty suspend(@PathVariable String propertyId, @RequestParam @Size(max = 300) String reason, Authentication auth) {
        return lifecycle.suspend(auth.getName(), propertyId, reason);
    }
}
