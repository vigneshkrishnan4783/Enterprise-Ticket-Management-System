package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.model.SlaPauseRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaPauseRuleRepository extends JpaRepository<SlaPauseRule, Long> {

  Optional<SlaPauseRule> findByStatusAndIsActiveTrue(TicketStatus status);

  Optional<SlaPauseRule> findByStatus(TicketStatus status);

  List<SlaPauseRule> findAllByIsActiveTrue();
}
