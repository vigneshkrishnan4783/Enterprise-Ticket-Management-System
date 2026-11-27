package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.EscalationRule;
import com.enterprise.ticket.model.EscalationTriggerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {

  List<EscalationRule> findByIsActiveTrue();

  List<EscalationRule> findByIsActiveTrueAndTriggerType(EscalationTriggerType triggerType);
}
