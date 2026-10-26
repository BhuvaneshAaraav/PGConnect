package com.pgconnect.support;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Collection;

public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {
    List<SupportTicket> findTop100ByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<SupportTicket> findTop500ByOrderByCreatedAtAsc();
    long countByStatusIn(Collection<SupportTicketStatus> statuses);
}
