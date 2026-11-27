package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketSatisfactionFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketSatisfactionFeedbackRepository
    extends JpaRepository<TicketSatisfactionFeedback, Long> {

  Optional<TicketSatisfactionFeedback> findByTicketId(Long ticketId);

  boolean existsByTicketId(Long ticketId);
}
