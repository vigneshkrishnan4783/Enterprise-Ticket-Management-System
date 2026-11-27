package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.SubmitFeedbackRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.FeedbackResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.TicketFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketNumber}/feedback")
@RequiredArgsConstructor
@Tag(
    name = "Customer Satisfaction Feedback",
    description = "Endpoints for submitting and viewing post-resolution CSAT feedback")
public class TicketFeedbackController {

  private final TicketFeedbackService feedbackService;

  @PostMapping
  @Operation(
      summary = "Submit satisfaction feedback",
      description =
          "Captures customer satisfaction rating (1-5) and optional comment after ticket resolution or closure")
  public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
      @PathVariable String ticketNumber,
      @Valid @RequestBody SubmitFeedbackRequest request,
      Principal principal) {
    return ResponseBuilder.<FeedbackResponse>builder()
        .created("Satisfaction feedback submitted successfully")
        .withData(feedbackService.submitFeedback(ticketNumber, request, principal.getName()))
        .build();
  }

  @GetMapping
  @Operation(
      summary = "Get satisfaction feedback",
      description = "Retrieves submitted customer satisfaction feedback for a ticket")
  public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(
      @PathVariable String ticketNumber, Principal principal) {
    return ResponseBuilder.<FeedbackResponse>builder()
        .success()
        .withData(feedbackService.getFeedback(ticketNumber, principal.getName()))
        .build();
  }
}
