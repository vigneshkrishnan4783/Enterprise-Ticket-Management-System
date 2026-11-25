package com.enterprise.ticket.service;

import com.enterprise.ticket.model.AssignmentRule;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.AssignmentRuleRepository;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentRuleEngine {

  private final AssignmentRuleRepository assignmentRuleRepository;

  @Getter
  @Builder
  public static class RuleEvaluationResult {
    private final Team targetTeam;
    private final User targetAgent;
    private final AssignmentRule matchedRule;
    private final String reason;
  }

    @Transactional(readOnly = true)
  public Optional<RuleEvaluationResult> evaluate(Ticket ticket) {
    if (ticket == null) {
      return Optional.empty();
    }

    List<AssignmentRule> activeRules =
        assignmentRuleRepository.findByIsActiveTrueOrderByPriorityOrderAsc();

    for (AssignmentRule rule : activeRules) {
      if (matches(rule, ticket)) {
        String reason =
            (rule.getReasonTemplate() != null && !rule.getReasonTemplate().trim().isEmpty())
                ? rule.getReasonTemplate().trim()
                : "Auto-assigned by rule: " + rule.getName();

        log.debug("Ticket {} matched assignment rule '{}'", ticket.getTicketNumber(), rule.getName());

        return Optional.of(
            RuleEvaluationResult.builder()
                .targetTeam(rule.getTargetTeam())
                .targetAgent(rule.getTargetAgent())
                .matchedRule(rule)
                .reason(reason)
                .build());
      }
    }

    return Optional.empty();
  }

    private boolean matches(AssignmentRule rule, Ticket ticket) {
    if (rule.getCategory() != null) {
      if (ticket.getCategory() == null || !rule.getCategory().getId().equals(ticket.getCategory().getId())) {
        return false;
      }
    }
    if (rule.getSubcategory() != null) {
      if (ticket.getSubcategory() == null
          || !rule.getSubcategory().getId().equals(ticket.getSubcategory().getId())) {
        return false;
      }
    }
    if (rule.getOrganization() != null) {
      if (ticket.getOrganization() == null
          || !rule.getOrganization().getId().equals(ticket.getOrganization().getId())) {
        return false;
      }
    }
    if (rule.getSite() != null) {
      if (ticket.getSite() == null || !rule.getSite().getId().equals(ticket.getSite().getId())) {
        return false;
      }
    }
    if (rule.getImpact() != null) {
      if (ticket.getImpact() == null || !rule.getImpact().getId().equals(ticket.getImpact().getId())) {
        return false;
      }
    }
    if (rule.getUrgency() != null) {
      if (ticket.getUrgency() == null || !rule.getUrgency().getId().equals(ticket.getUrgency().getId())) {
        return false;
      }
    }
    if (rule.getPriority() != null) {
      if (ticket.getPriority() == null || rule.getPriority() != ticket.getPriority()) {
        return false;
      }
    }
    if (rule.getChannel() != null) {
      if (ticket.getChannel() == null || rule.getChannel() != ticket.getChannel()) {
        return false;
      }
    }

    return true;
  }
}
