package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateEscalationRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.EscalationRuleResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.EscalationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/escalation-rules")
@RequiredArgsConstructor
@Tag(name = "Escalation Rules", description = "Endpoints for managing ticket escalation rules and policies")
public class EscalationRuleController {

  private final EscalationService escalationService;

  @PostMapping
  @Operation(summary = "Create escalation rule", description = "Creates a new escalation rule based on SLA breach or priority criteria")
  public ResponseEntity<ApiResponse<EscalationRuleResponse>> createRule(
      @Valid @RequestBody CreateEscalationRuleRequest request) {
    return ResponseBuilder.<EscalationRuleResponse>builder()
        .created("Escalation rule created successfully")
        .withData(escalationService.createRule(request))
        .build();
  }

  @GetMapping
  @Operation(summary = "Get all active escalation rules", description = "Retrieves all active escalation rules")
  public ResponseEntity<ApiResponse<List<EscalationRuleResponse>>> getActiveRules() {
    return ResponseBuilder.<List<EscalationRuleResponse>>builder()
        .success()
        .withData(escalationService.getActiveRules())
        .build();
  }
}
