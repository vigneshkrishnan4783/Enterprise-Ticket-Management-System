package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.model.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository
    extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

  Optional<Ticket> findByTicketNumber(String ticketNumber);

  Page<Ticket> findByReporterId(Long reporterId, Pageable pageable);

  List<Ticket> findByStatus(TicketStatus status);

  @Query(value = "SELECT nextval('ticket_seq')", nativeQuery = true)
  Long getNextTicketSequence();
}
