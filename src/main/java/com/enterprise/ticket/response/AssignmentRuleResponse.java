package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketChannel;
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
public class AssignmentRuleResponse {
  private Long id;
  private String name;
  private String description;
  private Integer priorityOrder;
  private Boolean isActive;
  private Long categoryId;
  private String categoryName;
  private Long subcategoryId;
  private String subcategoryName;
  private Long organizationId;
  private String organizationName;
  private Long siteId;
  private String siteName;
  private Long impactId;
  private String impactName;
  private Long urgencyId;
  private String urgencyName;
  private TicketPriority priority;
  private TicketChannel channel;
  private Long targetTeamId;
  private String targetTeamName;
  private Long targetAgentId;
  private String targetAgentUsername;
  private String reasonTemplate;

  private Instant createdAt;
}
