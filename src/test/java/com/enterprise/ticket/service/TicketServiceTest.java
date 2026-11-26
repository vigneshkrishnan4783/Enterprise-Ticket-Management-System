package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.request.CreateTicketRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

  @Test
  @DisplayName("createTicket should validate empty payload")
  void testCreateTicketNullPayload() {
    TicketService service =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    RestException ex = assertThrows(RestException.class, () -> service.createTicket(null, "user"));
    assertEquals("Create ticket payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("createTicket should validate short title")
  void testCreateTicketShortTitle() {
    TicketService service =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    CreateTicketRequest req = CreateTicketRequest.builder().title("abc").description("Valid description").build();
    RestException ex = assertThrows(RestException.class, () -> service.createTicket(req, "user"));
    assertEquals("Title is required and must be at least 5 characters", ex.getMessage());
  }

  @Test
  @DisplayName("createTicket should validate empty description")
  void testCreateTicketEmptyDescription() {
    TicketService service =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    CreateTicketRequest req = CreateTicketRequest.builder().title("Valid Title Here").description("  ").build();
    RestException ex = assertThrows(RestException.class, () -> service.createTicket(req, "user"));
    assertEquals("Description is required", ex.getMessage());
  }

  @Test
  @DisplayName("getTicketByNumber should validate null or empty ticket number")
  void testGetTicketNullNumber() {
    TicketService service =
        new TicketService(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    RestException ex = assertThrows(RestException.class, () -> service.getTicketByNumber("   ", "user"));
    assertEquals("Ticket number is required", ex.getMessage());
  }
}
