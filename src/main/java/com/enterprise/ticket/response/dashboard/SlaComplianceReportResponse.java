package com.enterprise.ticket.response.dashboard;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaComplianceReportResponse {

  private Instant from;
  private Instant to;
  private long totalTickets;
  private long responseMetCount;
  private long responseBreachedCount;
  private double responseCompliancePercent;
  private long resolutionMetCount;
  private long resolutionBreachedCount;
  private double resolutionCompliancePercent;
  private long currentlyPausedCount;
}
