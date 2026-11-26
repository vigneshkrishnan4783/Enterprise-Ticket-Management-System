package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.request.CreateAssignmentRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.AssignmentRuleResponse;
import com.enterprise.ticket.service.AssignmentRuleService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AssignmentRuleControllerTest {

  @Test
  @DisplayName("getAllActiveRules should return list of active assignment rules")
  void testGetAllActiveRules() {
    AssignmentRuleService dummyService =
        new AssignmentRuleService(null, null, null, null, null, null, null, null, null) {
          @Override
          public List<AssignmentRuleResponse> getAllActiveRules() {
            return List.of(
                AssignmentRuleResponse.builder()
                    .id(1L)
                    .name("Network Rule")
                    .priorityOrder(10)
                    .isActive(true)
                    .build());
          }
        };

    AssignmentRuleController controller = new AssignmentRuleController(dummyService);
    ResponseEntity<ApiResponse<List<AssignmentRuleResponse>>> response =
        controller.getAllActiveRules();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals("Network Rule", response.getBody().getData().get(0).getName());
  }

  @Test
  @DisplayName("createRule should return 201 Created and newly created rule response")
  void testCreateRule() {
    AssignmentRuleService dummyService =
        new AssignmentRuleService(null, null, null, null, null, null, null, null, null) {
          @Override
          public AssignmentRuleResponse createRule(CreateAssignmentRuleRequest request) {
            return AssignmentRuleResponse.builder()
                .id(2L)
                .name(request.getName())
                .priorityOrder(request.getPriorityOrder())
                .isActive(true)
                .build();
          }
        };

    AssignmentRuleController controller = new AssignmentRuleController(dummyService);
    CreateAssignmentRuleRequest request =
        CreateAssignmentRuleRequest.builder().name("Critical Escalation").priorityOrder(5).build();

    ResponseEntity<ApiResponse<AssignmentRuleResponse>> response = controller.createRule(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("Critical Escalation", response.getBody().getData().getName());
  }
}
