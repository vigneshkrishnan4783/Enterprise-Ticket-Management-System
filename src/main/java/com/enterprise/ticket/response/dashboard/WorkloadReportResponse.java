package com.enterprise.ticket.response.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadReportResponse {

  private long unassignedTickets;
  private List<AgentWorkloadItem> agents;

  public record AgentWorkloadItem(
      Long agentId,
      String agentUsername,
      long totalAssigned,
      long activeCount,
      long resolvedOrClosedCount,
      long slaBreachedCount,
      long escalatedCount,
      Double averageCsatRating) {}
}
