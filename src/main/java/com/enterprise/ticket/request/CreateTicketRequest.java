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
public class CreateTicketRequest {

  private String title;
  private String description;
  private TicketPriority priority;
  private TicketChannel channel;

  private Long organizationId;
  private Long siteId;
  private Long assetId;

  private Long categoryId;
  private Long subcategoryId;
  private Long impactId;
  private Long urgencyId;
}
