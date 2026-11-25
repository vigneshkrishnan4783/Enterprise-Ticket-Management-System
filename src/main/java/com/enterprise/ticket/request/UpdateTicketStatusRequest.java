package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {
  private TicketStatus status;
  private String reason;
}
