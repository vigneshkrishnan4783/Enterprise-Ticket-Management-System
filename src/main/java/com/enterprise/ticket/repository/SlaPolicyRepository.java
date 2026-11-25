package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.SlaPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

  Optional<SlaPolicy> findByPriorityAndIsActiveTrue(TicketPriority priority);
}
