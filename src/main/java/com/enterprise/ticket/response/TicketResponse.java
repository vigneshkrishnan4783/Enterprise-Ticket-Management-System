package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketChannel;
import com.enterprise.ticket.constant.TicketPriority;
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
public class TicketResponse {

  private Long id;
  private String ticketNumber;
  private String title;
  private String description;
  private TicketStatus status;
  private TicketPriority priority;
  private TicketChannel channel;

  private String reporterUsername;
  private String organizationName;
  private String siteName;
  private String assetTag;
  private String assetName;

  private String categoryName;
  private String subcategoryName;
  private Long impactId;
  private String impactName;
  private Long urgencyId;
  private String urgencyName;
  private Long assignedTeamId;
  private String assignedTeamName;
  private Long assignedAgentId;
  private String assignedAgentUsername;
  private Instant assignedAt;
  private com.enterprise.ticket.constant.AssignmentSource assignmentSource;
  private String assignmentReason;

  private Instant createdAt;
  private Instant updatedAt;
}
