package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaPauseRuleResponse {

  private Long id;
  private TicketStatus status;
  private Boolean pausesResponseSla;
  private Boolean pausesResolutionSla;
  private Boolean isActive;
  private Instant createdAt;
}
