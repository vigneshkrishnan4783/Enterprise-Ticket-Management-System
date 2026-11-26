package com.enterprise.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  @DisplayName("SlaCalculatorService should calculate fallback deadlines based on priority")
  void testSlaCalculator() {
    SlaPolicyRepository dummyRepo = new DummySlaPolicyRepository();
    SlaCalculatorService service = new SlaCalculatorService(dummyRepo);
    Ticket ticket = Ticket.builder().priority(TicketPriority.CRITICAL).build();

    service.calculateDeadlines(ticket);

    assertNotNull(ticket.getResponseDueAt());
    assertNotNull(ticket.getResolutionDueAt());
    assertTrue(ticket.getResolutionDueAt().isAfter(ticket.getResponseDueAt()));
  }

  static class DummySlaPolicyRepository implements SlaPolicyRepository {
    @Override
    public Optional<com.enterprise.ticket.model.SlaPolicy> findByPriorityAndIsActiveTrue(TicketPriority priority) {
      return Optional.empty();
    }
    @Override public void flush() {}
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> S saveAndFlush(S entity) { return null; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
    @Override public void deleteAllInBatch(Iterable<com.enterprise.ticket.model.SlaPolicy> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public com.enterprise.ticket.model.SlaPolicy getOne(Long aLong) { return null; }
    @Override public com.enterprise.ticket.model.SlaPolicy getById(Long aLong) { return null; }
    @Override public com.enterprise.ticket.model.SlaPolicy getReferenceById(Long aLong) { return null; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return java.util.Collections.emptyList(); }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return java.util.Collections.emptyList(); }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> long count(org.springframework.data.domain.Example<S> example) { return 0; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> S save(S entity) { return null; }
    @Override public <S extends com.enterprise.ticket.model.SlaPolicy> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
    @Override public Optional<com.enterprise.ticket.model.SlaPolicy> findById(Long aLong) { return Optional.empty(); }
    @Override public boolean existsById(Long aLong) { return false; }
    @Override public java.util.List<com.enterprise.ticket.model.SlaPolicy> findAll() { return java.util.Collections.emptyList(); }
    @Override public java.util.List<com.enterprise.ticket.model.SlaPolicy> findAllById(Iterable<Long> longs) { return java.util.Collections.emptyList(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long aLong) {}
    @Override public void delete(com.enterprise.ticket.model.SlaPolicy entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends com.enterprise.ticket.model.SlaPolicy> entities) {}
    @Override public void deleteAll() {}
    @Override public java.util.List<com.enterprise.ticket.model.SlaPolicy> findAll(org.springframework.data.domain.Sort sort) { return java.util.Collections.emptyList(); }
    @Override public org.springframework.data.domain.Page<com.enterprise.ticket.model.SlaPolicy> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
  }
}
