package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSlaPauseIntervalResponse {

  private Long id;
  private String ticketNumber;
  private TicketStatus status;
  private Instant pausedAt;
  private Instant resumedAt;
  private Long durationMinutes;
  private String pausedByUsername;
  private String resumedByUsername;
  private String pauseReason;
  private Instant createdAt;
}
