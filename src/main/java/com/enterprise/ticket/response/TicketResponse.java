package com.enterprise.ticket.response;

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
  private String category;
  private String reporterUsername;
  private String reporterOrganization;
  private Instant createdAt;
  private Instant updatedAt;
}
