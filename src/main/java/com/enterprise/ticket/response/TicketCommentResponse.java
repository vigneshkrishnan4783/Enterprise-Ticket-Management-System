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
public class TicketCommentResponse {
  private Long id;
  private Long ticketId;
  private String ticketNumber;
  private Long authorId;
  private String authorUsername;
  private String authorFullName;
  private String body;
  private Boolean isInternal;
  private Instant createdAt;
}
