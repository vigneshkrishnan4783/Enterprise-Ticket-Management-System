package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketSlaPauseInterval;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketSlaPauseIntervalRepository
    extends JpaRepository<TicketSlaPauseInterval, Long> {

  List<TicketSlaPauseInterval> findByTicketIdOrderByPausedAtAsc(Long ticketId);

  Optional<TicketSlaPauseInterval> findFirstByTicketIdAndResumedAtIsNullOrderByPausedAtDesc(
      Long ticketId);
}
