package com.enterprise.ticket.request;

import com.enterprise.ticket.constant.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

  @NotBlank(message = "Ticket title is required")
  @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
  private String title;

  @NotBlank(message = "Description is required")
  private String description;

  private TicketPriority priority;

  private String category;
}
