package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.EscalationRule;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketEscalationHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.EscalationRuleRepository;
import com.enterprise.ticket.repository.TeamRepository;
import com.enterprise.ticket.repository.TicketEscalationHistoryRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateEscalationRuleRequest;
import com.enterprise.ticket.request.EscalateTicketRequest;
import com.enterprise.ticket.response.EscalationRuleResponse;
import com.enterprise.ticket.response.TicketEscalationHistoryResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EscalationService {

  private final EscalationRuleRepository ruleRepository;
  private final TicketEscalationHistoryRepository historyRepository;
  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final NotificationService notificationService;

  @Transactional
  public EscalationRuleResponse createRule(CreateEscalationRuleRequest request) {
    if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
      throw RestException.badRequest("Rule name is required");
    }
    if (request.getTriggerType() == null) {
      throw RestException.badRequest("Trigger type is required");
    }

    Team targetTeam = null;
    if (request.getTargetTeamId() != null) {
      targetTeam =
          teamRepository
              .findById(request.getTargetTeamId())
              .orElseThrow(() -> RestException.notFound("Team not found with ID: " + request.getTargetTeamId()));
    }

    User targetAgent = null;
    if (request.getTargetAgentId() != null) {
      targetAgent =
          userRepository
              .findById(request.getTargetAgentId())
              .orElseThrow(() -> RestException.notFound("Agent not found with ID: " + request.getTargetAgentId()));
    }

    EscalationRule rule =
        EscalationRule.builder()
            .name(request.getName().trim())
            .triggerType(request.getTriggerType())
            .priority(request.getPriority())
            .targetLevel(request.getTargetLevel() != null ? request.getTargetLevel() : 1)
            .targetTeam(targetTeam)
            .targetAgent(targetAgent)
            .isActive(true)
            .build();

    return toRuleResponse(ruleRepository.save(rule));
  }

  @Transactional(readOnly = true)
  public List<EscalationRuleResponse> getActiveRules() {
    return ruleRepository.findByIsActiveTrue().stream().map(this::toRuleResponse).toList();
  }

  @Transactional
  public TicketEscalationHistoryResponse escalateTicket(
      String ticketNumber, EscalateTicketRequest request, String username) {
    if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
      throw RestException.badRequest("Escalation reason is required");
    }

    User caller = findUser(username);
    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(() -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "escalate");

    int fromLevel = ticket.getEscalationLevel() != null ? ticket.getEscalationLevel() : 0;
    int toLevel = request.getTargetLevel() != null && request.getTargetLevel() > fromLevel ? request.getTargetLevel() : fromLevel + 1;

    Team previousTeam = ticket.getAssignedTeam();
    User previousAgent = ticket.getAssignedAgent();

    Team newTeam = previousTeam;
    if (request.getTargetTeamId() != null) {
      newTeam =
          teamRepository
              .findById(request.getTargetTeamId())
              .orElseThrow(() -> RestException.notFound("Target team not found with ID: " + request.getTargetTeamId()));
      ticket.setAssignedTeam(newTeam);
    }

    User newAgent = previousAgent;
    if (request.getTargetAgentId() != null) {
      newAgent =
          userRepository
              .findById(request.getTargetAgentId())
              .orElseThrow(() -> RestException.notFound("Target agent not found with ID: " + request.getTargetAgentId()));
      ticket.setAssignedAgent(newAgent);
    }

    EscalationRule rule = null;
    if (request.getRuleId() != null) {
      rule = ruleRepository.findById(request.getRuleId()).orElse(null);
    }

    ticket.setEscalationLevel(toLevel);
    ticket.setIsEscalated(true);
    ticketRepository.save(ticket);

    TicketEscalationHistory history =
        TicketEscalationHistory.builder()
            .ticket(ticket)
            .rule(rule)
            .fromLevel(fromLevel)
            .toLevel(toLevel)
            .reason(request.getReason().trim())
            .escalatedBy(caller)
            .previousTeam(previousTeam)
            .newTeam(newTeam)
            .previousAgent(previousAgent)
            .newAgent(newAgent)
            .build();

    TicketEscalationHistory savedHistory = historyRepository.save(history);

    if (notificationService != null) {
      String escalationMsg =
          "Ticket " + ticket.getTicketNumber() + " has been escalated to Level " + toLevel + ": " + request.getReason().trim();
      if (ticket.getAssignedAgent() != null) {
        notificationService.createNotification(
            ticket.getAssignedAgent(), ticket, NotificationEventType.STATUS_CHANGED, "Ticket Escalated: " + ticket.getTicketNumber(), escalationMsg);
      }
      notificationService.createNotification(
          ticket.getReporter(), ticket, NotificationEventType.STATUS_CHANGED, "Ticket Escalated: " + ticket.getTicketNumber(), escalationMsg);
    }

    return toHistoryResponse(savedHistory);
  }

  @Transactional(readOnly = true)
  public List<TicketEscalationHistoryResponse> getEscalationHistory(String ticketNumber, String username) {
    User caller = findUser(username);
    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(() -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "view escalation history of");
    return historyRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
        .map(this::toHistoryResponse)
        .toList();
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> RestException.notFound("User not found: " + username));
  }

  private void enforceTicketAccess(Ticket ticket, User caller, String action) {
    Set<RoleType> roles = caller.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
    if (roles.contains(RoleType.ROLE_ADMIN)) {
      return;
    }
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
      if (!isReporter) {
        throw RestException.forbidden("Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
      }
      return;
    }
    boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
    boolean isAssignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(caller.getId());
    boolean isAssignedTeamMember =
        ticket.getAssignedTeam() != null
            && ticket.getAssignedTeam().getMembers().stream().anyMatch(m -> m.getId().equals(caller.getId()));
    if (!isReporter && !isAssignedAgent && !isAssignedTeamMember && !roles.contains(RoleType.ROLE_AGENT)) {
      throw RestException.forbidden("Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
    }
  }

  private EscalationRuleResponse toRuleResponse(EscalationRule r) {
    return EscalationRuleResponse.builder()
        .id(r.getId())
        .name(r.getName())
        .triggerType(r.getTriggerType())
        .priority(r.getPriority())
        .targetLevel(r.getTargetLevel())
        .targetTeamId(r.getTargetTeam() != null ? r.getTargetTeam().getId() : null)
        .targetTeamName(r.getTargetTeam() != null ? r.getTargetTeam().getName() : null)
        .targetAgentId(r.getTargetAgent() != null ? r.getTargetAgent().getId() : null)
        .targetAgentUsername(r.getTargetAgent() != null ? r.getTargetAgent().getUsername() : null)
        .isActive(r.getIsActive())
        .createdAt(r.getCreatedAt())
        .build();
  }

  private TicketEscalationHistoryResponse toHistoryResponse(TicketEscalationHistory h) {
    return TicketEscalationHistoryResponse.builder()
        .id(h.getId())
        .ticketNumber(h.getTicket().getTicketNumber())
        .ruleId(h.getRule() != null ? h.getRule().getId() : null)
        .ruleName(h.getRule() != null ? h.getRule().getName() : null)
        .fromLevel(h.getFromLevel())
        .toLevel(h.getToLevel())
        .reason(h.getReason())
        .escalatedByUsername(h.getEscalatedBy() != null ? h.getEscalatedBy().getUsername() : null)
        .previousTeamName(h.getPreviousTeam() != null ? h.getPreviousTeam().getName() : null)
        .newTeamName(h.getNewTeam() != null ? h.getNewTeam().getName() : null)
        .previousAgentUsername(h.getPreviousAgent() != null ? h.getPreviousAgent().getUsername() : null)
        .newAgentUsername(h.getNewAgent() != null ? h.getNewAgent().getUsername() : null)
        .createdAt(h.getCreatedAt())
        .build();
  }
}
