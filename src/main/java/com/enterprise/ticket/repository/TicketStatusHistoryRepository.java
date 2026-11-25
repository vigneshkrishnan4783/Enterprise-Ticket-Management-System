package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

  List<TicketStatusHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
