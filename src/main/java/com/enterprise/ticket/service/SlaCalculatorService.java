package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.SlaPolicy;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.repository.SlaPolicyRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlaCalculatorService {

  private final SlaPolicyRepository slaPolicyRepository;

  public void calculateDeadlines(Ticket ticket) {
    if (ticket == null || ticket.getPriority() == null) {
      return;
    }

    Instant baseline = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now();

    Optional<SlaPolicy> policyOpt =
        slaPolicyRepository.findByPriorityAndIsActiveTrue(ticket.getPriority());

    if (policyOpt.isPresent()) {
      SlaPolicy policy = policyOpt.get();
      ticket.setResponseDueAt(baseline.plus(policy.getResponseTimeMinutes(), ChronoUnit.MINUTES));
      ticket.setResolutionDueAt(baseline.plus(policy.getResolutionTimeMinutes(), ChronoUnit.MINUTES));
    } else {
      ticket.setResponseDueAt(baseline.plus(getDefaultResponseMinutes(ticket.getPriority()), ChronoUnit.MINUTES));
      ticket.setResolutionDueAt(baseline.plus(getDefaultResolutionMinutes(ticket.getPriority()), ChronoUnit.MINUTES));
    }
  }

  public void recalculateOnPriorityChange(Ticket ticket, TicketPriority newPriority) {
    if (ticket == null || newPriority == null) {
      return;
    }

    ticket.setPriority(newPriority);
    Instant now = Instant.now();

    Optional<SlaPolicy> policyOpt = slaPolicyRepository.findByPriorityAndIsActiveTrue(newPriority);

    if (policyOpt.isPresent()) {
      SlaPolicy policy = policyOpt.get();
      if (ticket.getFirstRespondedAt() == null) {
        ticket.setResponseDueAt(now.plus(policy.getResponseTimeMinutes(), ChronoUnit.MINUTES));
      }
      if (ticket.getResolvedAt() == null) {
        ticket.setResolutionDueAt(now.plus(policy.getResolutionTimeMinutes(), ChronoUnit.MINUTES));
      }
    }
  }

  private int getDefaultResponseMinutes(TicketPriority priority) {
    return switch (priority) {
      case CRITICAL -> 15;
      case HIGH -> 60;
      case MEDIUM -> 240;
      case LOW -> 480;
    };
  }

  private int getDefaultResolutionMinutes(TicketPriority priority) {
    return switch (priority) {
      case CRITICAL -> 240;
      case HIGH -> 480;
      case MEDIUM -> 1440;
      case LOW -> 2880;
    };
  }
}
