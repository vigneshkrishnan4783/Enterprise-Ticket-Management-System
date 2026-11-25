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
public class TicketStatusHistoryResponse {
  private Long id;
  private Long ticketId;
  private String ticketNumber;
  private TicketStatus previousStatus;
  private TicketStatus newStatus;
  private Long changedById;
  private String changedByUsername;
  private String reason;
  private Instant createdAt;
}
