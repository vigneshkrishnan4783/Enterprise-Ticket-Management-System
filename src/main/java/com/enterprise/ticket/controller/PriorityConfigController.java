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
    return ResponseBuilder.ok(priorityConfigService.getAllImpacts());
  }

  @PostMapping("/impacts")
  @Operation(summary = "Create impact level", description = "Adds a new impact definition")
  public ResponseEntity<ApiResponse<ImpactResponse>> createImpact(
      @RequestBody CreateImpactRequest request) {
    return ResponseBuilder.created("Impact created successfully", priorityConfigService.createImpact(request));
  }

  @GetMapping("/urgencies")
  @Operation(summary = "Get all active urgencies", description = "Returns active configurable urgency levels ordered by urgency weight")
  public ResponseEntity<ApiResponse<List<UrgencyResponse>>> getAllUrgencies() {
    return ResponseBuilder.ok(priorityConfigService.getAllUrgencies());
  }

  @PostMapping("/urgencies")
  @Operation(summary = "Create urgency level", description = "Adds a new urgency definition")
  public ResponseEntity<ApiResponse<UrgencyResponse>> createUrgency(
      @RequestBody CreateUrgencyRequest request) {
    return ResponseBuilder.created("Urgency created successfully", priorityConfigService.createUrgency(request));
  }

  @GetMapping("/matrix")
  @Operation(summary = "Get priority matrix mappings", description = "Returns dynamic Impact x Urgency -> Priority mapping table")
  public ResponseEntity<ApiResponse<List<PriorityMatrixResponse>>> getPriorityMatrix() {
    return ResponseBuilder.ok(priorityConfigService.getPriorityMatrix());
  }

  @PutMapping("/matrix")
  @Operation(summary = "Configure priority matrix rule", description = "Maps an Impact and Urgency combination to a calculated Priority")
  public ResponseEntity<ApiResponse<PriorityMatrixResponse>> configureMatrix(
      @RequestBody ConfigurePriorityMatrixRequest request) {
    return ResponseBuilder.ok("Priority matrix updated successfully", priorityConfigService.configureMatrix(request));
  }
}
