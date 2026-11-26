package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.service.TicketService;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TicketControllerTest {

  @Test
  @DisplayName("createTicket should return 201 Created and created TicketResponse")
  void testCreateTicket() {
    TicketService dummyTicketService =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null) {
          @Override
          public TicketResponse createTicket(CreateTicketRequest request, String username) {
            return TicketResponse.builder()
                .id(100L)
                .ticketNumber("TKT-10001")
                .title(request.getTitle())
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .build();
          }
        };

    TicketController controller = new TicketController(dummyTicketService);
    CreateTicketRequest request =
        CreateTicketRequest.builder().title("Database connection timeout").build();

    Principal principal = () -> "test_user";
    ResponseEntity<ApiResponse<TicketResponse>> response =
        controller.createTicket(request, principal);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("TKT-10001", response.getBody().getData().getTicketNumber());
    assertEquals("Database connection timeout", response.getBody().getData().getTitle());
  }

  @Test
  @DisplayName("getTicket should return 200 OK and matching ticket details")
  void testGetTicket() {
    TicketService dummyTicketService =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null) {
          @Override
          public TicketResponse getTicketByNumber(String ticketNumber, String username) {
            return TicketResponse.builder()
                .id(100L)
                .ticketNumber(ticketNumber)
                .title("Network outage")
                .status(TicketStatus.IN_PROGRESS)
                .build();
          }
        };

    TicketController controller = new TicketController(dummyTicketService);
    Principal principal = () -> "agent_smith";
    ResponseEntity<ApiResponse<TicketResponse>> response =
        controller.getTicket("TKT-10001", principal);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("TKT-10001", response.getBody().getData().getTicketNumber());
  }
}
