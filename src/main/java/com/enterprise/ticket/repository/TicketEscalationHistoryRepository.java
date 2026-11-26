package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketEscalationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketEscalationHistoryRepository extends JpaRepository<TicketEscalationHistory, Long> {

  List<TicketEscalationHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
