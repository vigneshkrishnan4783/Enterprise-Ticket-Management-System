package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketChannel;
import com.enterprise.ticket.constant.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRuleRequest {
  private String name;
  private String description;
  private Integer priorityOrder;
  private Long categoryId;
  private Long subcategoryId;
  private Long organizationId;
  private Long siteId;
  private Long impactId;
  private Long urgencyId;
  private TicketPriority priority;
  private TicketChannel channel;
  private Long targetTeamId;
  private Long targetAgentId;
  private String reasonTemplate;
}
