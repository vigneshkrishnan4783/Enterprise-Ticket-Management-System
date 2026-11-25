package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.AddCommentRequest;
import com.enterprise.ticket.request.UpdateTicketStatusRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.TicketCommentResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.response.TicketStatusHistoryResponse;
import com.enterprise.ticket.service.TicketTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketNumber}")
@RequiredArgsConstructor
@Tag(name = "Ticket Timeline", description = "Endpoints for ticket comments, notes, and status transition history")
public class TicketTimelineController {

  private final TicketTimelineService timelineService;

  @PostMapping("/comments")
  @Operation(summary = "Add comment or private note", description = "Adds a public reply or private internal note to a ticket")
  public ResponseEntity<ApiResponse<TicketCommentResponse>> addComment(
      @PathVariable String ticketNumber,
      @RequestBody AddCommentRequest request,
      Principal principal) {
    return ResponseBuilder.<TicketCommentResponse>builder()
        .created("Comment added successfully")
        .withData(timelineService.addComment(ticketNumber, request, principal.getName()))
        .build();
  }

  @GetMapping("/comments")
  @Operation(summary = "Get chronological comments", description = "Retrieves comments in chronological order. Internal notes are hidden from external customers.")
  public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<List<TicketCommentResponse>>builder()
        .success()
        .withData(timelineService.getComments(ticketNumber, principal.getName()))
        .build();
  }

  @PutMapping("/status")
  @Operation(summary = "Update ticket status", description = "Updates ticket status and records state change in status history")
  public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
      @PathVariable String ticketNumber,
      @RequestBody UpdateTicketStatusRequest request,
      Principal principal) {
    return ResponseBuilder.<TicketResponse>builder()
        .success("Status updated successfully")
        .withData(timelineService.updateStatus(ticketNumber, request, principal.getName()))
        .build();
  }

  @GetMapping("/status-history")
  @Operation(summary = "Get status history", description = "Retrieves chronological status transition audit history for a ticket")
  public ResponseEntity<ApiResponse<List<TicketStatusHistoryResponse>>> getStatusHistory(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<List<TicketStatusHistoryResponse>>builder()
        .success()
        .withData(timelineService.getStatusHistory(ticketNumber, principal.getName()))
        .build();
  }
}
