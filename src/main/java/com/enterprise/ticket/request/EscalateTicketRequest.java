package com.enterprise.ticket.request;

import jakarta.validation.constraints.NotBlank;
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
public class EscalateTicketRequest {
  @NotBlank(message = "Escalation reason is required")
  private String reason;

  private Integer targetLevel;
  private Long targetTeamId;
  private Long targetAgentId;
  private Long ruleId;
}
