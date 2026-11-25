package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

  List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

  List<TicketComment> findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(Long ticketId);
}
