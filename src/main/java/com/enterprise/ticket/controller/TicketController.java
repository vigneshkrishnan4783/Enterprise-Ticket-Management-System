package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
      @Valid @RequestBody CreateTicketRequest request, Principal principal) {
    TicketResponse response = ticketService.createTicket(request, principal.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Ticket created successfully", response));
  }

  @GetMapping("/{ticketNumber}")
  public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable String ticketNumber) {
    TicketResponse response = ticketService.getTicketByNumber(ticketNumber);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/my")
  public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(
      Principal principal, @PageableDefault(size = 20) Pageable pageable) {
    Page<TicketResponse> response = ticketService.getMyTickets(principal.getName(), pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
