package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.SlaPauseRule;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketSlaPauseInterval;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.SlaPauseRuleRepository;
import com.enterprise.ticket.repository.TicketSlaPauseIntervalRepository;
import com.enterprise.ticket.request.CreateSlaPauseRuleRequest;
import com.enterprise.ticket.response.SlaPauseRuleResponse;
import com.enterprise.ticket.response.TicketSlaPauseIntervalResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlaPauseService {

  private final SlaPauseRuleRepository ruleRepository;
  private final TicketSlaPauseIntervalRepository intervalRepository;

  @Transactional
  public SlaPauseRuleResponse createOrUpdateRule(CreateSlaPauseRuleRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create SLA pause rule payload cannot be empty");
    }
    if (request.getStatus() == null) {
      throw RestException.badRequest("Ticket status is required for pause rule");
    }

    SlaPauseRule rule =
        ruleRepository
            .findByStatus(request.getStatus())
            .orElseGet(() -> SlaPauseRule.builder().status(request.getStatus()).build());

    rule.setPausesResponseSla(Boolean.TRUE.equals(request.getPausesResponseSla()));
    rule.setPausesResolutionSla(request.getPausesResolutionSla() == null || Boolean.TRUE.equals(request.getPausesResolutionSla()));
    rule.setIsActive(request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive()));

    return toRuleResponse(ruleRepository.save(rule));
  }

  @Transactional(readOnly = true)
  public List<SlaPauseRuleResponse> getAllActiveRules() {
    return ruleRepository.findAllByIsActiveTrue().stream()
        .map(this::toRuleResponse)
        .toList();
  }

  @Transactional
  public void handleStatusTransition(
      Ticket ticket,
      TicketStatus previousStatus,
      TicketStatus newStatus,
      User actor,
      String reason) {
    if (ticket == null || previousStatus == null || newStatus == null) {
      return;
    }

    Instant now = Instant.now();

    Optional<SlaPauseRule> prevRuleOpt = ruleRepository.findByStatusAndIsActiveTrue(previousStatus);
    if (prevRuleOpt.isPresent()) {
      intervalRepository
          .findFirstByTicketIdAndResumedAtIsNullOrderByPausedAtDesc(ticket.getId())
          .ifPresent(
              interval -> {
                interval.setResumedAt(now);
                interval.setResumedBy(actor);
                long minutes = Duration.between(interval.getPausedAt(), now).toMinutes();
                interval.setDurationMinutes(minutes);
                intervalRepository.save(interval);

                long currentTotal =
                    ticket.getTotalPausedDurationMinutes() != null
                        ? ticket.getTotalPausedDurationMinutes()
                        : 0L;
                ticket.setTotalPausedDurationMinutes(currentTotal + minutes);

                if (Boolean.TRUE.equals(prevRuleOpt.get().getPausesResolutionSla())
                    && ticket.getResolutionDueAt() != null) {
                  ticket.setResolutionDueAt(ticket.getResolutionDueAt().plus(minutes, ChronoUnit.MINUTES));
                }

                if (Boolean.TRUE.equals(prevRuleOpt.get().getPausesResponseSla())
                    && ticket.getResponseDueAt() != null
                    && ticket.getFirstRespondedAt() == null) {
                  ticket.setResponseDueAt(ticket.getResponseDueAt().plus(minutes, ChronoUnit.MINUTES));
                }

                ticket.setIsSlaPaused(false);
              });
    }

    Optional<SlaPauseRule> newRuleOpt = ruleRepository.findByStatusAndIsActiveTrue(newStatus);
    if (newRuleOpt.isPresent()) {
      TicketSlaPauseInterval interval =
          TicketSlaPauseInterval.builder()
              .ticket(ticket)
              .status(newStatus)
              .pausedAt(now)
              .pausedBy(actor)
              .pauseReason(reason)
              .createdAt(now)
              .build();
      intervalRepository.save(interval);

      ticket.setIsSlaPaused(true);
      ticket.setSlaLastPausedAt(now);
    }
  }

  @Transactional(readOnly = true)
  public List<TicketSlaPauseIntervalResponse> getPauseIntervals(Long ticketId, String ticketNumber) {
    return intervalRepository.findByTicketIdOrderByPausedAtAsc(ticketId).stream()
        .map(i -> toIntervalResponse(i, ticketNumber))
        .toList();
  }

  private SlaPauseRuleResponse toRuleResponse(SlaPauseRule r) {
    return SlaPauseRuleResponse.builder()
        .id(r.getId())
        .status(r.getStatus())
        .pausesResponseSla(r.getPausesResponseSla())
        .pausesResolutionSla(r.getPausesResolutionSla())
        .isActive(r.getIsActive())
        .createdAt(r.getCreatedAt())
        .build();
  }

  private TicketSlaPauseIntervalResponse toIntervalResponse(
      TicketSlaPauseInterval i, String ticketNumber) {
    return TicketSlaPauseIntervalResponse.builder()
        .id(i.getId())
        .ticketNumber(ticketNumber)
        .status(i.getStatus())
        .pausedAt(i.getPausedAt())
        .resumedAt(i.getResumedAt())
        .durationMinutes(i.getDurationMinutes())
        .pausedByUsername(i.getPausedBy() != null ? i.getPausedBy().getUsername() : null)
        .resumedByUsername(i.getResumedBy() != null ? i.getResumedBy().getUsername() : null)
        .pauseReason(i.getPauseReason())
        .createdAt(i.getCreatedAt())
        .build();
  }
}
