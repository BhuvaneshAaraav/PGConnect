package com.pgconnect.support;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SupportTicketController {
    private final SupportTicketService service;
    public SupportTicketController(SupportTicketService service) { this.service = service; }

    @GetMapping("/api/support/tickets")
    public List<SupportTicket> myTickets(Authentication auth) { return service.myTickets(auth.getName()); }

    @PostMapping("/api/support/tickets")
    public SupportTicket create(@Valid @RequestBody SupportTicketService.CreateTicketRequest request, Authentication auth) {
        return service.create(auth.getName(), request);
    }

    @GetMapping("/api/admin/support/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SupportTicketService.AdminTicketView> adminTickets() { return service.adminTickets(); }

    @PatchMapping("/api/admin/support/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SupportTicket update(@PathVariable String id, @Valid @RequestBody SupportTicketService.UpdateTicketRequest request,
                                Authentication auth) {
        return service.update(auth.getName(), id, request);
    }
}
