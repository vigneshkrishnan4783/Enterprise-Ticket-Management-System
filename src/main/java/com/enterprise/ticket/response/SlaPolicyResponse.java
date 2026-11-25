package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaPolicyResponse {

  private Long id;
  private TicketPriority priority;
  private Integer responseTimeMinutes;
  private Integer resolutionTimeMinutes;
  private Boolean isActive;
}
