package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.TicketLifecyclePolicyRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.TicketLifecyclePolicyResponse;
import com.enterprise.ticket.service.TicketLifecyclePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lifecycle-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
    name = "Lifecycle Policies",
    description = "Admin endpoints for customer reopening and ticket auto-closure policies")
public class TicketLifecyclePolicyController {

  private final TicketLifecyclePolicyService lifecyclePolicyService;

  @PostMapping
  @Operation(
      summary = "Create or update lifecycle policy",
      description =
          "Configures global or organization-specific customer reopen window, max reopens, and auto-closure hours")
  public ResponseEntity<ApiResponse<TicketLifecyclePolicyResponse>> createOrUpdatePolicy(
      @Valid @RequestBody TicketLifecyclePolicyRequest request) {
    return ResponseBuilder.<TicketLifecyclePolicyResponse>builder()
        .success("Lifecycle policy saved successfully")
        .withData(lifecyclePolicyService.createOrUpdatePolicy(request))
        .build();
  }

  @GetMapping
  @Operation(
      summary = "List lifecycle policies",
      description = "Retrieves all configured customer reopening and auto-closure policies")
  public ResponseEntity<ApiResponse<List<TicketLifecyclePolicyResponse>>> getAllPolicies() {
    return ResponseBuilder.<List<TicketLifecyclePolicyResponse>>builder()
        .success()
        .withData(lifecyclePolicyService.getAllPolicies())
        .build();
  }

  @PostMapping("/auto-close/run")
  @Operation(
      summary = "Run auto-closure job",
      description =
          "Manually triggers the auto-closure check for resolved tickets past their policy window")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> runAutoClosures() {
    int closedCount = lifecyclePolicyService.processAutoClosures();
    return ResponseBuilder.<Map<String, Integer>>builder()
        .success("Auto-closure check completed")
        .withData(Map.of("closedCount", closedCount))
        .build();
  }
}
