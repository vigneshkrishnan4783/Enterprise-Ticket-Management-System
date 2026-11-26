package com.enterprise.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.repository.SlaPolicyRepository;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.SlaCalculatorService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PipelineVerificationTest {

  @Test
  @DisplayName("ResponseBuilder should return standardized API response with data and status")
  void testResponseBuilderSuccess() {
    ResponseEntity<ApiResponse<String>> response =
        ResponseBuilder.<String>builder()
            .success()
            .withData("Verification Successful")
            .build();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("Verification Successful", response.getBody().getData());
  }

  @Test
  @DisplayName("RestException should sanitize message before super call in Java 25")
  void testRestExceptionJava25() {
    RestException ex = RestException.badRequest("   Custom bad request   ");
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals("Custom bad request", ex.getMessage());

    RestException defaultEx = new RestException(null, "  ");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, defaultEx.getStatus());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), defaultEx.getMessage());
  }

  @Test
  @DisplayName("SlaCalculatorService should calculate deadlines based on priority")
  void testSlaCalculator() {
    SlaPolicyRepository mockRepo = Mockito.mock(SlaPolicyRepository.class);
    Mockito.when(mockRepo.findByPriorityAndIsActiveTrue(Mockito.any())).thenReturn(Optional.empty());

    SlaCalculatorService service = new SlaCalculatorService(mockRepo);
    Ticket ticket = Ticket.builder().priority(TicketPriority.CRITICAL).build();

    service.calculateDeadlines(ticket);

    assertNotNull(ticket.getResponseDueAt());
    assertNotNull(ticket.getResolutionDueAt());
    assertTrue(ticket.getResolutionDueAt().isAfter(ticket.getResponseDueAt()));
  }
}
