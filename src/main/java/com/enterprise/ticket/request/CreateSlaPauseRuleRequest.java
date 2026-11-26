package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketStatus;
import jakarta.validation.constraints.NotNull;
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
public class CreateSlaPauseRuleRequest {

  @NotNull(message = "Ticket status is required")
  private TicketStatus status;

  @Builder.Default
  private Boolean pausesResponseSla = false;

  @Builder.Default
  private Boolean pausesResolutionSla = true;

  @Builder.Default
  private Boolean isActive = true;
}
