package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse.AgentWorkloadItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface DashboardReportRepository {

  DashboardAggregate getSystemAggregate();

  DashboardAggregate getAgentAggregate(Long agentId);

  long countTeamQueueOpenTickets(Long agentId);

  DashboardAggregate getCustomerAggregate(Long customerId, String organizationName);

  DashboardAggregate getReporterAggregate(Long reporterId);

  SlaAggregate getSlaAggregate(Instant from, Instant to);

  long countUnassignedTickets();

  List<AgentWorkloadItem> getAgentWorkload();

  SummaryAggregate getSummaryAggregate(Instant from, Instant to);

  record DashboardAggregate(
      long totalTickets,
      long escalatedTickets,
      long slaBreachedTickets,
      Double averageCsatRating,
      long totalFeedbackCount,
      Map<String, Long> ticketsByStatus,
      Map<String, Long> ticketsByPriority) {}

  record SlaAggregate(
      long totalTickets,
      long responseBreachedCount,
      long resolutionBreachedCount,
      long currentlyPausedCount) {}

  record SummaryAggregate(
      long totalTickets,
      long totalEscalations,
      Double averageCsatRating,
      long totalFeedbackCount,
      Map<String, Long> ticketsByStatus,
      Map<String, Long> ticketsByPriority,
      Map<String, Long> ticketsByCategory,
      Map<Integer, Long> csatRatingDistribution) {}

  static long countByStatus(Map<String, Long> byStatus, TicketStatus status) {
    return byStatus.getOrDefault(status.name(), 0L);
  }
}
