package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.ConfigurePriorityMatrixRequest;
import com.enterprise.ticket.request.CreateImpactRequest;
import com.enterprise.ticket.request.CreateUrgencyRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ImpactResponse;
import com.enterprise.ticket.response.PriorityMatrixResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.UrgencyResponse;
import com.enterprise.ticket.service.PriorityConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/priority-config")
@RequiredArgsConstructor
@Tag(name = "Priority Configuration", description = "Endpoints for managing configurable Impact, Urgency, and Priority Matrix (FR-006)")
public class PriorityConfigController {

  private final PriorityConfigService priorityConfigService;

  @GetMapping("/impacts")
  @Operation(summary = "Get all active impacts", description = "Returns active configurable impact levels ordered by severity weight")
  public ResponseEntity<ApiResponse<List<ImpactResponse>>> getAllImpacts() {
    return ResponseBuilder.<List<ImpactResponse>>builder()
        .success()
        .withData(priorityConfigService.getAllImpacts())
        .build();
  }

  @PostMapping("/impacts")
  @Operation(summary = "Create impact level", description = "Adds a new impact definition")
  public ResponseEntity<ApiResponse<ImpactResponse>> createImpact(
      @RequestBody CreateImpactRequest request) {
    return ResponseBuilder.<ImpactResponse>builder()
        .created("Impact created successfully")
        .withData(priorityConfigService.createImpact(request))
        .build();
  }

  @GetMapping("/urgencies")
  @Operation(summary = "Get all active urgencies", description = "Returns active configurable urgency levels ordered by urgency weight")
  public ResponseEntity<ApiResponse<List<UrgencyResponse>>> getAllUrgencies() {
    return ResponseBuilder.<List<UrgencyResponse>>builder()
        .success()
        .withData(priorityConfigService.getAllUrgencies())
        .build();
  }

  @PostMapping("/urgencies")
  @Operation(summary = "Create urgency level", description = "Adds a new urgency definition")
  public ResponseEntity<ApiResponse<UrgencyResponse>> createUrgency(
      @RequestBody CreateUrgencyRequest request) {
    return ResponseBuilder.<UrgencyResponse>builder()
        .created("Urgency created successfully")
        .withData(priorityConfigService.createUrgency(request))
        .build();
  }

  @GetMapping("/matrix")
  @Operation(summary = "Get priority matrix mappings", description = "Returns dynamic Impact x Urgency -> Priority mapping table")
  public ResponseEntity<ApiResponse<List<PriorityMatrixResponse>>> getPriorityMatrix() {
    return ResponseBuilder.<List<PriorityMatrixResponse>>builder()
        .success()
        .withData(priorityConfigService.getPriorityMatrix())
        .build();
  }

  @PutMapping("/matrix")
  @Operation(summary = "Configure priority matrix rule", description = "Maps an Impact and Urgency combination to a calculated Priority")
  public ResponseEntity<ApiResponse<PriorityMatrixResponse>> configureMatrix(
      @RequestBody ConfigurePriorityMatrixRequest request) {
    return ResponseBuilder.<PriorityMatrixResponse>builder()
        .success("Priority matrix updated successfully")
        .withData(priorityConfigService.configureMatrix(request))
        .build();
  }
}
