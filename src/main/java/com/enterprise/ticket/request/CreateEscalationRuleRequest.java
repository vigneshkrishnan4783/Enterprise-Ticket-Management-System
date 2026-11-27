package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.EscalationTriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEscalationRuleRequest {
  @NotBlank(message = "Rule name is required")
  private String name;

  @NotNull(message = "Trigger type is required")
  private EscalationTriggerType triggerType;

  private TicketPriority priority;
  private Integer targetLevel;
  private Long targetTeamId;
  private Long targetAgentId;
}
