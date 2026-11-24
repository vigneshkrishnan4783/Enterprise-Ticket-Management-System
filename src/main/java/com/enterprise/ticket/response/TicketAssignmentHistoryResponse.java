package com.enterprise.ticket.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignmentHistoryResponse {
  private Long id;
  private Long ticketId;
  private String ticketNumber;
  private String previousTeamName;
  private String newTeamName;
  private String previousAgentUsername;
  private String newAgentUsername;
  private String assignedByUsername;
  private String comment;
  private Instant createdAt;
}
