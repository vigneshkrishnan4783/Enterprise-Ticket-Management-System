package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Endpoints for ticket management, creation and lookup")
public class TicketController {

  private final TicketService ticketService;

  @PostMapping
  @Operation(summary = "Create ticket", description = "Creates a ticket with unique human-readable ticket number (FR-002)")
  public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
      @RequestBody CreateTicketRequest request, Principal principal) {
    TicketResponse response = ticketService.createTicket(request, principal.getName());
    return ResponseBuilder.created("Ticket created successfully", response);
  }

  @GetMapping("/{ticketNumber}")
  @Operation(summary = "Get ticket by number", description = "Retrieves ticket details by human-readable ticket number")
  public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
      @PathVariable String ticketNumber) {
    TicketResponse response = ticketService.getTicketByNumber(ticketNumber);
    return ResponseBuilder.ok(response);
  }

  @GetMapping("/ticket")
  @Operation(summary = "Get tickets", description = "Returns paginated list of tickets reported by authenticated user")
  public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(
      Principal principal, @PageableDefault(size = 20) Pageable pageable) {
    Page<TicketResponse> response = ticketService.getMyTickets(principal.getName(), pageable);
    return ResponseBuilder.ok(response);
  }
}
