package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

  List<TicketAttachment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
