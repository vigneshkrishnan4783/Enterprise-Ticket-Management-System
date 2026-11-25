package com.enterprise.ticket.controller;

import com.enterprise.ticket.constant.TicketChannel;
import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.request.AssignTicketRequest;
import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.request.TicketSearchFilter;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.TicketAssignmentHistoryResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Endpoints for ticket management, creation, search, assignment and lookup")
public class TicketController {

  private final TicketService ticketService;

  @PostMapping
  @Operation(summary = "Create ticket", description = "Creates a ticket with scope, impact/urgency priority matrix calculation")
  public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
      @RequestBody CreateTicketRequest request, Principal principal) {
    return ResponseBuilder.<TicketResponse>builder()
        .created("Ticket created successfully")
        .withData(ticketService.createTicket(request, principal.getName()))
        .build();
  }

  @GetMapping("/{ticketNumber}")
  @Operation(summary = "Get ticket by number", description = "Retrieves ticket details with ticket-level data access enforcement")
  public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<TicketResponse>builder()
        .success()
        .withData(ticketService.getTicketByNumber(ticketNumber, principal.getName()))
        .build();
  }

  @GetMapping("/ticket")
  @Operation(summary = "Get tickets", description = "Returns paginated list of tickets reported by authenticated user")
  public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(
      Principal principal, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseBuilder.<Page<TicketResponse>>builder()
        .success()
        .withData(ticketService.getMyTickets(principal.getName(), pageable))
        .build();
  }

  @GetMapping("/search")
  @Operation(
      summary = "Search and filter tickets",
      description = "FR-007: Full ticket search, multi-field filtering, sorting, server-side pagination with data access enforcement")
  public ResponseEntity<ApiResponse<Page<TicketResponse>>> searchTickets(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) TicketPriority priority,
      @RequestParam(required = false) TicketChannel channel,
      @RequestParam(required = false) Long organizationId,
      @RequestParam(required = false) Long siteId,
      @RequestParam(required = false) Long assetId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long subcategoryId,
      @RequestParam(required = false) Long impactId,
      @RequestParam(required = false) Long urgencyId,
      @RequestParam(required = false) Long reporterId,
      @RequestParam(required = false) Instant createdAfter,
      @RequestParam(required = false) Instant createdBefore,
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    TicketSearchFilter filter =
        TicketSearchFilter.builder()
            .keyword(keyword)
            .status(status)
            .priority(priority)
            .channel(channel)
            .organizationId(organizationId)
            .siteId(siteId)
            .assetId(assetId)
            .categoryId(categoryId)
            .subcategoryId(subcategoryId)
            .impactId(impactId)
            .urgencyId(urgencyId)
            .reporterId(reporterId)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .build();

    return ResponseBuilder.<Page<TicketResponse>>builder()
        .success()
        .withData(ticketService.searchTickets(filter, principal.getName(), pageable))
        .build();
  }

  @PutMapping("/{ticketNumber}/assign")
  @Operation(summary = "Manual assignment / reassignment", description = "Assigns ticket to a support team and/or agent with audit history")
  public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
      @PathVariable String ticketNumber,
      @RequestBody AssignTicketRequest request,
      Principal principal) {
    return ResponseBuilder.<TicketResponse>builder()
        .success("Ticket assigned successfully")
        .withData(ticketService.assignTicket(ticketNumber, request, principal.getName()))
        .build();
  }

  @PostMapping("/{ticketNumber}/auto-assign")
  @Operation(summary = "Trigger automatic rule-based assignment", description = "Evaluates active assignment rules and auto-assigns ticket with source and reason audit")
  public ResponseEntity<ApiResponse<TicketResponse>> autoAssignTicket(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<TicketResponse>builder()
        .success("Ticket auto-assigned successfully")
        .withData(ticketService.autoAssignTicket(ticketNumber, principal.getName()))
        .build();
  }

  @GetMapping("/{ticketNumber}/history")
  @Operation(summary = "Get assignment history", description = "Retrieves full assignment and reassignment audit trail for a ticket")
  public ResponseEntity<ApiResponse<List<TicketAssignmentHistoryResponse>>> getAssignmentHistory(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<List<TicketAssignmentHistoryResponse>>builder()
        .success()
        .withData(ticketService.getAssignmentHistory(ticketNumber, principal.getName()))
        .build();
  }
}
