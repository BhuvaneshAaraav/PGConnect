package com.pgconnect.support;

import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.common.AuditLog;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final UserAccountRepository users;
    private final MongoTemplate mongo;

    public SupportTicketService(SupportTicketRepository tickets, UserAccountRepository users, MongoTemplate mongo) {
        this.tickets = tickets;
        this.users = users;
        this.mongo = mongo;
    }

    public List<SupportTicket> myTickets(String requesterId) {
        return tickets.findTop100ByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    public SupportTicket create(String requesterId, CreateTicketRequest request) {
        UserAccount requester = users.findById(requesterId).filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Enabled account required"));
        SupportTicket ticket = new SupportTicket();
        ticket.setRequesterId(requester.getId());
        ticket.setRequesterRole(requester.getRole());
        ticket.setCategory(request.category().trim());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        return tickets.save(ticket);
    }

    public List<AdminTicketView> adminTickets() {
        return tickets.findTop500ByOrderByCreatedAtAsc().stream().map(ticket -> {
            UserAccount requester = users.findById(ticket.getRequesterId()).orElse(null);
            return new AdminTicketView(ticket.getId(), ticket.getRequesterId(), ticket.getRequesterRole(),
                    requester == null ? "Account removed" : requester.getName(), requester == null ? null : requester.getEmailNormalized(),
                    ticket.getCategory(), ticket.getSubject(), ticket.getDescription(), ticket.getStatus(),
                    ticket.getAdminResponse(), ticket.getCreatedAt(), ticket.getUpdatedAt());
        }).toList();
    }

    public SupportTicket update(String adminId, String id, UpdateTicketRequest request) {
        SupportTicket ticket = tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found"));
        ticket.setStatus(request.status());
        ticket.setAdminResponse(request.response() == null ? null : request.response().trim());
        ticket.setReviewedBy(adminId);
        ticket.setUpdatedAt(Instant.now());
        SupportTicket saved = tickets.save(ticket);
        mongo.save(new AuditLog(adminId, "SUPPORT_TICKET_UPDATED", "SupportTicket", id,
                Map.of("status", request.status().name(), "requesterId", ticket.getRequesterId())));
        return saved;
    }

    public record CreateTicketRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=60) String category,
                                      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=140) String subject,
                                      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=3000) String description) {}
    public record UpdateTicketRequest(@jakarta.validation.constraints.NotNull SupportTicketStatus status,
                                      @jakarta.validation.constraints.Size(max=3000) String response) {}
    public record AdminTicketView(String id, String requesterId, com.pgconnect.auth.Role requesterRole,
                                  String requesterName, String requesterEmail, String category, String subject,
                                  String description, SupportTicketStatus status, String adminResponse,
                                  Instant createdAt, Instant updatedAt) {}
}
