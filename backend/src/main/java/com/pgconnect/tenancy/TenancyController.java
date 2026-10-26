package com.pgconnect.tenancy;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TenancyController {
    private final TenancyService service;
    public TenancyController(TenancyService service) { this.service = service; }

    @GetMapping("/api/resident/accommodation")
    @PreAuthorize("hasRole('RESIDENT')")
    public TenancyService.Accommodation current(Authentication auth) { return service.current(auth.getName()); }

    @GetMapping("/api/resident/accommodation/history")
    @PreAuthorize("hasRole('RESIDENT')")
    public List<TenancyService.Accommodation> history(Authentication auth) { return service.residentHistory(auth.getName()); }

    @PostMapping("/api/resident/accommodation/{tenancyId}/move-out")
    @PreAuthorize("hasRole('RESIDENT')")
    public Tenancy requestMoveOut(@PathVariable String tenancyId, @Valid @RequestBody TenancyService.MoveOutRequest request, Authentication auth) {
        return service.requestMoveOut(auth.getName(), tenancyId, request);
    }

    @GetMapping("/api/owner/properties/{propertyId}/residents")
    @PreAuthorize("hasRole('OWNER')")
    public List<TenancyService.Accommodation> ownerResidents(@PathVariable String propertyId, Authentication auth) {
        return service.ownerResidents(auth.getName(), propertyId);
    }

    @PostMapping("/api/owner/properties/{propertyId}/tenancies/{tenancyId}/move-out")
    @PreAuthorize("hasRole('OWNER')")
    public Tenancy decideMoveOut(@PathVariable String propertyId, @PathVariable String tenancyId,
                                 @Valid @RequestBody TenancyService.MoveOutDecision request, Authentication auth) {
        return service.completeMoveOut(auth.getName(), propertyId, tenancyId, request);
    }
}
