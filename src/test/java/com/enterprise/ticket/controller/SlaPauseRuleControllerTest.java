package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.request.CreateSlaPauseRuleRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.SlaPauseRuleResponse;
import com.enterprise.ticket.service.SlaPauseService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SlaPauseRuleControllerTest {

  @Test
  @DisplayName("getAllActiveRules should return 200 OK and list of active pause rules")
  void testGetAllActiveRules() {
    SlaPauseService dummyService =
        new SlaPauseService(null, null) {
          @Override
          public List<SlaPauseRuleResponse> getAllActiveRules() {
            return List.of(
                SlaPauseRuleResponse.builder()
                    .id(1L)
                    .status(TicketStatus.PENDING_CUSTOMER)
                    .pausesResolutionSla(true)
                    .pausesResponseSla(false)
                    .isActive(true)
                    .build());
          }
        };

    SlaPauseRuleController controller = new SlaPauseRuleController(dummyService);
    ResponseEntity<ApiResponse<List<SlaPauseRuleResponse>>> response =
        controller.getAllActiveRules();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals(TicketStatus.PENDING_CUSTOMER, response.getBody().getData().getFirst().getStatus());
  }

  @Test
  @DisplayName("createOrUpdateRule should return 201 Created and rule response")
  void testCreateOrUpdateRule() {
    SlaPauseService dummyService =
        new SlaPauseService(null, null) {
          @Override
          public SlaPauseRuleResponse createOrUpdateRule(CreateSlaPauseRuleRequest req) {
            return SlaPauseRuleResponse.builder()
                .id(2L)
                .status(req.getStatus())
                .pausesResolutionSla(req.getPausesResolutionSla())
                .build();
          }
        };

    SlaPauseRuleController controller = new SlaPauseRuleController(dummyService);
    CreateSlaPauseRuleRequest request =
        CreateSlaPauseRuleRequest.builder()
            .status(TicketStatus.PENDING_INTERNAL)
            .pausesResolutionSla(true)
            .build();

    ResponseEntity<ApiResponse<SlaPauseRuleResponse>> response =
        controller.createOrUpdateRule(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(2L, response.getBody().getData().getId());
    assertEquals(TicketStatus.PENDING_INTERNAL, response.getBody().getData().getStatus());
  }
}
