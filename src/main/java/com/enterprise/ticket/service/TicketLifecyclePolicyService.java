package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Organization;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketLifecyclePolicy;
import com.enterprise.ticket.model.TicketStatusHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.OrganizationRepository;
import com.enterprise.ticket.repository.TicketLifecyclePolicyRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketStatusHistoryRepository;
import com.enterprise.ticket.request.TicketLifecyclePolicyRequest;
import com.enterprise.ticket.response.TicketLifecyclePolicyResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketLifecyclePolicyService {

  private final TicketLifecyclePolicyRepository policyRepository;
  private final OrganizationRepository organizationRepository;
  private final TicketRepository ticketRepository;
  private final TicketStatusHistoryRepository statusHistoryRepository;
  private final NotificationService notificationService;

  @Autowired
  public TicketLifecyclePolicyService(
      TicketLifecyclePolicyRepository policyRepository,
      OrganizationRepository organizationRepository,
      TicketRepository ticketRepository,
      TicketStatusHistoryRepository statusHistoryRepository,
      @Autowired(required = false) NotificationService notificationService) {
    this.policyRepository = policyRepository;
    this.organizationRepository = organizationRepository;
    this.ticketRepository = ticketRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.notificationService = notificationService;
  }

  @Transactional
  public TicketLifecyclePolicyResponse createOrUpdatePolicy(TicketLifecyclePolicyRequest request) {
    if (request == null || request.getPolicyName() == null || request.getPolicyName().isBlank()) {
      throw RestException.badRequest("Policy name is required");
    }

    String normalizedName = request.getPolicyName().trim();
    TicketLifecyclePolicy policy =
        policyRepository
            .findByPolicyNameIgnoreCase(normalizedName)
            .orElseGet(TicketLifecyclePolicy::new);

    policy.setPolicyName(normalizedName);
    if (request.getOrganizationId() != null) {
      Organization org =
          organizationRepository
              .findById(request.getOrganizationId())
              .orElseThrow(
                  () ->
                      RestException.notFound(
                          "Organization not found with id: " + request.getOrganizationId()));
      policy.setOrganization(org);
    } else {
      policy.setOrganization(null);
    }

    if (request.getAllowCustomerReopen() != null) {
      policy.setAllowCustomerReopen(request.getAllowCustomerReopen());
    }
    if (request.getReopenWindowHours() != null) {
      policy.setReopenWindowHours(request.getReopenWindowHours());
    }
    if (request.getMaxReopenCount() != null) {
      policy.setMaxReopenCount(request.getMaxReopenCount());
    }
    if (request.getAutoCloseEnabled() != null) {
      policy.setAutoCloseEnabled(request.getAutoCloseEnabled());
    }
    if (request.getAutoCloseAfterHours() != null) {
      policy.setAutoCloseAfterHours(request.getAutoCloseAfterHours());
    }
    if (request.getIsActive() != null) {
      policy.setIsActive(request.getIsActive());
    }

    return toResponse(policyRepository.save(policy));
  }

  @Transactional(readOnly = true)
  public List<TicketLifecyclePolicyResponse> getAllPolicies() {
    return policyRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public TicketLifecyclePolicy getEffectivePolicy(Organization organization) {
    if (organization != null && organization.getId() != null) {
      var orgPolicy =
          policyRepository.findFirstByOrganizationIdAndIsActiveTrue(organization.getId());
      if (orgPolicy.isPresent()) {
        return orgPolicy.get();
      }
    }
    return policyRepository
        .findFirstByOrganizationIsNullAndIsActiveTrue()
        .orElseGet(
            () ->
                TicketLifecyclePolicy.builder()
                    .policyName("DEFAULT_FALLBACK")
                    .allowCustomerReopen(true)
                    .reopenWindowHours(72)
                    .maxReopenCount(3)
                    .autoCloseEnabled(true)
                    .autoCloseAfterHours(72)
                    .isActive(true)
                    .build());
  }

  public void validateAndRecordReopen(Ticket ticket, User caller) {
    if (ticket.getStatus() != TicketStatus.RESOLVED
        && ticket.getStatus() != TicketStatus.CLOSED) {
      throw RestException.badRequest("Only RESOLVED or CLOSED tickets can be reopened");
    }

    TicketLifecyclePolicy policy = getEffectivePolicy(ticket.getOrganization());

    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      if (!Boolean.TRUE.equals(policy.getAllowCustomerReopen())) {
        throw RestException.forbidden("Customer reopening is disabled by lifecycle policy");
      }

      if (policy.getReopenWindowHours() != null
          && policy.getReopenWindowHours() > 0
          && ticket.getResolvedAt() != null) {
        Instant deadline =
            ticket.getResolvedAt().plus(policy.getReopenWindowHours(), ChronoUnit.HOURS);
        if (Instant.now().isAfter(deadline)) {
          throw RestException.badRequest(
              "Customer reopen window of " + policy.getReopenWindowHours() + " hours has expired");
        }
      }

      int currentReopens = ticket.getReopenCount() != null ? ticket.getReopenCount() : 0;
      if (policy.getMaxReopenCount() != null
          && policy.getMaxReopenCount() > 0
          && currentReopens >= policy.getMaxReopenCount()) {
        throw RestException.badRequest(
            "Maximum customer reopen limit of " + policy.getMaxReopenCount() + " reached");
      }
    }

    int nextCount = (ticket.getReopenCount() != null ? ticket.getReopenCount() : 0) + 1;
    ticket.setReopenCount(nextCount);
    ticket.setClosedAt(null);
  }

  @Scheduled(fixedDelayString = "${app.lifecycle.auto-close-interval-ms:3600000}")
  @Transactional
  public int processAutoClosures() {
    List<Ticket> resolvedTickets = ticketRepository.findByStatus(TicketStatus.RESOLVED);
    Instant now = Instant.now();
    int closedCount = 0;

    for (Ticket ticket : resolvedTickets) {
      if (ticket.getResolvedAt() == null) {
        continue;
      }
      TicketLifecyclePolicy policy = getEffectivePolicy(ticket.getOrganization());
      if (!Boolean.TRUE.equals(policy.getAutoCloseEnabled())
          || policy.getAutoCloseAfterHours() == null) {
        continue;
      }

      Instant closeAt =
          ticket.getResolvedAt().plus(policy.getAutoCloseAfterHours(), ChronoUnit.HOURS);
      if (now.isAfter(closeAt)) {
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(now);
        ticketRepository.save(ticket);

        String reason =
            "Auto-closed by lifecycle policy after "
                + policy.getAutoCloseAfterHours()
                + " hours";
        TicketStatusHistory history =
            TicketStatusHistory.builder()
                .ticket(ticket)
                .previousStatus(TicketStatus.RESOLVED)
                .newStatus(TicketStatus.CLOSED)
                .reason(reason)
                .build();
        statusHistoryRepository.save(history);

        if (notificationService != null && ticket.getReporter() != null) {
          notificationService.createNotification(
              ticket.getReporter(),
              ticket,
              NotificationEventType.STATUS_CHANGED,
              "Ticket " + ticket.getTicketNumber() + " auto-closed",
              reason);
        }
        closedCount++;
      }
    }

    return closedCount;
  }

  private TicketLifecyclePolicyResponse toResponse(TicketLifecyclePolicy policy) {
    return TicketLifecyclePolicyResponse.builder()
        .id(policy.getId())
        .policyName(policy.getPolicyName())
        .organizationId(policy.getOrganization() != null ? policy.getOrganization().getId() : null)
        .organizationName(
            policy.getOrganization() != null ? policy.getOrganization().getName() : null)
        .allowCustomerReopen(policy.getAllowCustomerReopen())
        .reopenWindowHours(policy.getReopenWindowHours())
        .maxReopenCount(policy.getMaxReopenCount())
        .autoCloseEnabled(policy.getAutoCloseEnabled())
        .autoCloseAfterHours(policy.getAutoCloseAfterHours())
        .isActive(policy.getIsActive())
        .createdAt(policy.getCreatedAt())
        .updatedAt(policy.getUpdatedAt())
        .build();
  }
}
