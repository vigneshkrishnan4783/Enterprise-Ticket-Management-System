package com.enterprise.ticket.response.dashboard;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalSummaryReportResponse {

  private Instant from;
  private Instant to;
  private long totalTickets;
  private long totalEscalations;
  private Double averageCsatRating;
  private long totalFeedbackCount;
  private Map<String, Long> ticketsByStatus;
  private Map<String, Long> ticketsByPriority;
  private Map<String, Long> ticketsByCategory;
  private Map<Integer, Long> csatRatingDistribution;
}
