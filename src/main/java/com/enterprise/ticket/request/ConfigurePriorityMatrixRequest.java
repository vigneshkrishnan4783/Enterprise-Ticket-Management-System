package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurePriorityMatrixRequest {
  private Long impactId;
  private Long urgencyId;
  private TicketPriority priority;
}
