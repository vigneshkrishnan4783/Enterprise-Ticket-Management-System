package com.enterprise.ticket.response.dashboard;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDashboardResponse {

  private String username;
  private String role;
  private String scope;
  private long totalTickets;
  private long openTickets;
  private long inProgressTickets;
  private long pendingTickets;
  private long resolvedTickets;
  private long closedTickets;
  private long reopenedTickets;
  private long escalatedTickets;
  private long slaBreachedTickets;
  private long teamQueueOpenTickets;
  private Double averageCsatRating;
  private long totalFeedbackCount;
  private Map<String, Long> ticketsByStatus;
  private Map<String, Long> ticketsByPriority;
}
