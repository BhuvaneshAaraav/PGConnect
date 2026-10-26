package com.pgconnect.support;

import com.pgconnect.auth.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("support_tickets")
@CompoundIndex(name = "ticket_requester_time", def = "{'requesterId':1,'createdAt':-1}")
@CompoundIndex(name = "ticket_status_time", def = "{'status':1,'createdAt':1}")
public class SupportTicket {
    @Id private String id;
    private String requesterId;
    private Role requesterRole;
    private String category;
    private String subject;
    private String description;
    private SupportTicketStatus status = SupportTicketStatus.OPEN;
    private String adminResponse;
    private String reviewedBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public Role getRequesterRole() { return requesterRole; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public SupportTicketStatus getStatus() { return status; }
    public String getAdminResponse() { return adminResponse; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setRequesterId(String value) { requesterId = value; }
    public void setRequesterRole(Role value) { requesterRole = value; }
    public void setCategory(String value) { category = value; }
    public void setSubject(String value) { subject = value; }
    public void setDescription(String value) { description = value; }
    public void setStatus(SupportTicketStatus value) { status = value; }
    public void setAdminResponse(String value) { adminResponse = value; }
    public void setReviewedBy(String value) { reviewedBy = value; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
