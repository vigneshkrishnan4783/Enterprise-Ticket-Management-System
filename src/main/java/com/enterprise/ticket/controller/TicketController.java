package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing ticket creation, retrieval, and tracking.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Endpoints for ticket management, creation and lookup")
public class TicketController {

  private final TicketService ticketService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create ticket", description = "Creates a ticket with unique human-readable ticket number (FR-002)")
  public ApiResponse<TicketResponse> createTicket(
      @RequestBody CreateTicketRequest request, Principal principal) {
    log.info("Creating ticket '{}' for reporter: {}", request.getTitle(), principal.getName());
    return ApiResponse.success("Ticket created successfully", ticketService.createTicket(request, principal.getName()));
  }

  @GetMapping("/{ticketNumber}")
  @Operation(summary = "Get ticket by number", description = "Retrieves ticket details by human-readable ticket number")
  public ApiResponse<TicketResponse> getTicket(@PathVariable String ticketNumber) {
    return ApiResponse.success(ticketService.getTicketByNumber(ticketNumber));
  }

  @GetMapping("/my")
  @Operation(summary = "Get my tickets", description = "Returns paginated list of tickets reported by authenticated user")
  public ApiResponse<Page<TicketResponse>> getMyTickets(
      Principal principal, @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.success(ticketService.getMyTickets(principal.getName(), pageable));
  }
}
