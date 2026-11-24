package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketPriority;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityMatrixResponse {
  private Long id;
  private Long impactId;
  private String impactName;
  private String impactCode;
  private Long urgencyId;
  private String urgencyName;
  private String urgencyCode;
  private TicketPriority priority;
  private Boolean isActive;
  private Instant createdAt;
}
