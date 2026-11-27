package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.EscalationTriggerType;
import com.enterprise.ticket.request.CreateEscalationRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.EscalationRuleResponse;
import com.enterprise.ticket.service.EscalationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EscalationRuleControllerTest {

  @Test
  @DisplayName("createRule should return 201 Created and rule response")
  void testCreateRule() {
    EscalationService dummyService =
        new EscalationService(null, null, null, null, null, null) {
          @Override
          public EscalationRuleResponse createRule(CreateEscalationRuleRequest request) {
            return EscalationRuleResponse.builder()
                .id(1L)
                .name(request.getName())
                .triggerType(request.getTriggerType())
                .targetLevel(1)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
          }
        };

    EscalationRuleController controller = new EscalationRuleController(dummyService);
    CreateEscalationRuleRequest request =
        CreateEscalationRuleRequest.builder()
            .name("SLA Resolution Breach Rule")
            .triggerType(EscalationTriggerType.SLA_RESOLUTION_BREACHED)
            .priority(TicketPriority.HIGH)
            .targetLevel(1)
            .build();

    ResponseEntity<ApiResponse<EscalationRuleResponse>> response = controller.createRule(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("SLA Resolution Breach Rule", response.getBody().getData().name());
  }

  @Test
  @DisplayName("getActiveRules should return 200 OK and active rules list")
  void testGetActiveRules() {
    EscalationService dummyService =
        new EscalationService(null, null, null, null, null, null) {
          @Override
          public List<EscalationRuleResponse> getActiveRules() {
            return List.of(
                EscalationRuleResponse.builder()
                    .id(1L)
                    .name("Rule 1")
                    .triggerType(EscalationTriggerType.MANUAL)
                    .targetLevel(1)
                    .isActive(true)
                    .build());
          }
        };

    EscalationRuleController controller = new EscalationRuleController(dummyService);
    ResponseEntity<ApiResponse<List<EscalationRuleResponse>>> response = controller.getActiveRules();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
  }
}
