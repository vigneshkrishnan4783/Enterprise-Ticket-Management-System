package com.enterprise.ticket.service.dashboard;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.DashboardReportRepository;
import com.enterprise.ticket.repository.DashboardReportRepository.DashboardAggregate;
import com.enterprise.ticket.repository.DashboardReportRepository.SlaAggregate;
import com.enterprise.ticket.repository.DashboardReportRepository.SummaryAggregate;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.dashboard.OperationalSummaryReportResponse;
import com.enterprise.ticket.response.dashboard.RoleDashboardResponse;
import com.enterprise.ticket.response.dashboard.SlaComplianceReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardReportService {

  private final DashboardReportRepository reportRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public RoleDashboardResponse getAdminDashboard(String username) {
    User caller = findUser(username);
    DashboardAggregate agg = reportRepository.getSystemAggregate();
    return buildDashboard(
        caller.getUsername(), RoleType.ROLE_ADMIN.name(), "SYSTEM_WIDE", agg, 0L);
  }

  @Transactional(readOnly = true)
  public RoleDashboardResponse getAgentDashboard(String username) {
    User caller = findUser(username);
    DashboardAggregate agg = reportRepository.getAgentAggregate(caller.getId());
    long teamQueueOpen = reportRepository.countTeamQueueOpenTickets(caller.getId());
    return buildDashboard(
        caller.getUsername(), RoleType.ROLE_AGENT.name(), "AGENT_AND_TEAM", agg, teamQueueOpen);
  }

  @Transactional(readOnly = true)
  public RoleDashboardResponse getCustomerDashboard(String username) {
    User caller = findUser(username);
    DashboardAggregate agg =
        reportRepository.getCustomerAggregate(caller.getId(), caller.getOrganizationName());
    return buildDashboard(
        caller.getUsername(),
        RoleType.ROLE_CUSTOMER.name(),
        "CUSTOMER_ORGANIZATION",
        agg,
        0L);
  }

  @Transactional(readOnly = true)
  public RoleDashboardResponse getEmployeeDashboard(String username) {
    User caller = findUser(username);
    DashboardAggregate agg = reportRepository.getReporterAggregate(caller.getId());
    return buildDashboard(
        caller.getUsername(), RoleType.ROLE_EMPLOYEE.name(), "REPORTER_OWN", agg, 0L);
  }

  @Transactional(readOnly = true)
  public RoleDashboardResponse getMyDashboard(String username) {
    User caller = findUser(username);
    if (hasRole(caller, RoleType.ROLE_ADMIN)) {
      return getAdminDashboard(username);
    }
    if (hasRole(caller, RoleType.ROLE_AGENT)) {
      return getAgentDashboard(username);
    }
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER
        || hasRole(caller, RoleType.ROLE_CUSTOMER)) {
      return getCustomerDashboard(username);
    }
    return getEmployeeDashboard(username);
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> RestException.notFound("User not found: " + username));
  }

  @Transactional(readOnly = true)
  public SlaComplianceReportResponse getSlaComplianceReport(Instant from, Instant to) {
    validateDateRange(from, to);
    SlaAggregate agg = reportRepository.getSlaAggregate(from, to);
    long total = agg.totalTickets();
    long responseMet = Math.max(0L, total - agg.responseBreachedCount());
    long resolutionMet = Math.max(0L, total - agg.resolutionBreachedCount());

    return SlaComplianceReportResponse.builder()
        .from(from)
        .to(to)
        .totalTickets(total)
        .responseMetCount(responseMet)
        .responseBreachedCount(agg.responseBreachedCount())
        .responseCompliancePercent(calculatePercent(responseMet, total))
        .resolutionMetCount(resolutionMet)
        .resolutionBreachedCount(agg.resolutionBreachedCount())
        .resolutionCompliancePercent(calculatePercent(resolutionMet, total))
        .currentlyPausedCount(agg.currentlyPausedCount())
        .build();
  }

  @Transactional(readOnly = true)
  public WorkloadReportResponse getWorkloadReport() {
    return WorkloadReportResponse.builder()
        .unassignedTickets(reportRepository.countUnassignedTickets())
        .agents(reportRepository.getAgentWorkload())
        .build();
  }

  @Transactional(readOnly = true)
  public OperationalSummaryReportResponse getOperationalSummaryReport(Instant from, Instant to) {
    validateDateRange(from, to);
    SummaryAggregate agg = reportRepository.getSummaryAggregate(from, to);
    return OperationalSummaryReportResponse.builder()
        .from(from)
        .to(to)
        .totalTickets(agg.totalTickets())
        .totalEscalations(agg.totalEscalations())
        .averageCsatRating(agg.averageCsatRating())
        .totalFeedbackCount(agg.totalFeedbackCount())
        .ticketsByStatus(agg.ticketsByStatus())
        .ticketsByPriority(agg.ticketsByPriority())
        .ticketsByCategory(agg.ticketsByCategory())
        .csatRatingDistribution(agg.csatRatingDistribution())
        .build();
  }

  private RoleDashboardResponse buildDashboard(
      String username, String role, String scope, DashboardAggregate agg, long teamQueueOpen) {
    long pendingCount =
        DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.PENDING_CUSTOMER)
            + DashboardReportRepository.countByStatus(
                agg.ticketsByStatus(), TicketStatus.PENDING_INTERNAL);

    long openCount =
        DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.OPEN)
            + DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.ASSIGNED);

    return RoleDashboardResponse.builder()
        .username(username)
        .role(role)
        .scope(scope)
        .totalTickets(agg.totalTickets())
        .openTickets(openCount)
        .inProgressTickets(
            DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.IN_PROGRESS))
        .pendingTickets(pendingCount)
        .resolvedTickets(
            DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.RESOLVED))
        .closedTickets(
            DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.CLOSED))
        .reopenedTickets(
            DashboardReportRepository.countByStatus(agg.ticketsByStatus(), TicketStatus.REOPENED))
        .escalatedTickets(agg.escalatedTickets())
        .slaBreachedTickets(agg.slaBreachedTickets())
        .teamQueueOpenTickets(teamQueueOpen)
        .averageCsatRating(agg.averageCsatRating())
        .totalFeedbackCount(agg.totalFeedbackCount())
        .ticketsByStatus(agg.ticketsByStatus())
        .ticketsByPriority(agg.ticketsByPriority())
        .build();
  }

  private boolean hasRole(User user, RoleType roleType) {
    return user.getRoles() != null
        && user.getRoles().stream().anyMatch(r -> r.getName() == roleType);
  }

  private void validateDateRange(Instant from, Instant to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw RestException.badRequest("'from' timestamp cannot be after 'to' timestamp");
    }
  }

  private double calculatePercent(long part, long total) {
    if (total <= 0) {
      return 100.0;
    }
    return Math.round((part * 10000.0) / total) / 100.0;
  }
}
