package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketAssignmentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketAssignmentHistoryRepository
    extends JpaRepository<TicketAssignmentHistory, Long> {
  List<TicketAssignmentHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
