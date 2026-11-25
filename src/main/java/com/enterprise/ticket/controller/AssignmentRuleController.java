package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateAssignmentRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.AssignmentRuleResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.AssignmentRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignment-rules")
@RequiredArgsConstructor
@Tag(name = "Assignment Rules", description = "Endpoints for managing configurable automatic ticket assignment rules (FR-005)")
public class AssignmentRuleController {

  private final AssignmentRuleService assignmentRuleService;

  @GetMapping
  @Operation(summary = "List all active assignment rules", description = "Retrieves active automatic assignment rules ordered by priority")
  public ResponseEntity<ApiResponse<List<AssignmentRuleResponse>>> getAllActiveRules() {
    return ResponseBuilder.<List<AssignmentRuleResponse>>builder()
        .success()
        .withData(assignmentRuleService.getAllActiveRules())
        .build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get assignment rule by ID", description = "Retrieves details of a specific assignment rule")
  public ResponseEntity<ApiResponse<AssignmentRuleResponse>> getRuleById(@PathVariable Long id) {
    return ResponseBuilder.<AssignmentRuleResponse>builder()
        .success()
        .withData(assignmentRuleService.getRuleById(id))
        .build();
  }

  @PostMapping
  @Operation(summary = "Create assignment rule", description = "Creates a new automatic ticket assignment rule with matching criteria and routing destination")
  public ResponseEntity<ApiResponse<AssignmentRuleResponse>> createRule(
      @RequestBody CreateAssignmentRuleRequest request) {
    return ResponseBuilder.<AssignmentRuleResponse>builder()
        .created("Assignment rule created successfully")
        .withData(assignmentRuleService.createRule(request))
        .build();
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deactivate assignment rule", description = "Deactivates an existing assignment rule")
  public ResponseEntity<ApiResponse<Void>> deactivateRule(@PathVariable Long id) {
    assignmentRuleService.deactivateRule(id);
    return ResponseBuilder.<Void>builder()
        .success("Assignment rule deactivated successfully")
        .build();
  }
}
