package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateSlaPauseRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.SlaPauseRuleResponse;
import com.enterprise.ticket.service.SlaPauseService;
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
@RequestMapping("/api/v1/sla-pause-rules")
@RequiredArgsConstructor
@Tag(name = "SLA Pause Rules", description = "Endpoints for configuring which ticket statuses pause SLA clocks")
public class SlaPauseRuleController {

  private final SlaPauseService slaPauseService;

  @PostMapping
  @Operation(summary = "Create or update SLA pause rule", description = "Configures whether a ticket status pauses first-response and/or resolution SLA")
  public ResponseEntity<ApiResponse<SlaPauseRuleResponse>> createOrUpdateRule(
      @Valid @RequestBody CreateSlaPauseRuleRequest request) {
    return ResponseBuilder.<SlaPauseRuleResponse>builder()
        .created("SLA pause rule configured successfully")
        .withData(slaPauseService.createOrUpdateRule(request))
        .build();
  }

  @GetMapping
  @Operation(summary = "Get all active SLA pause rules", description = "Retrieves all active SLA pause rules")
  public ResponseEntity<ApiResponse<List<SlaPauseRuleResponse>>> getAllActiveRules() {
    return ResponseBuilder.<List<SlaPauseRuleResponse>>builder()
        .success()
        .withData(slaPauseService.getAllActiveRules())
        .build();
  }
}
