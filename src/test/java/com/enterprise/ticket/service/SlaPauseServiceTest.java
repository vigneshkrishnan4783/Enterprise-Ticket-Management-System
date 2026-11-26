package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.SlaPauseRule;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketSlaPauseInterval;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.SlaPauseRuleRepository;
import com.enterprise.ticket.repository.TicketSlaPauseIntervalRepository;
import com.enterprise.ticket.request.CreateSlaPauseRuleRequest;
import com.enterprise.ticket.response.SlaPauseRuleResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlaPauseServiceTest {

  @Test
  @DisplayName("createOrUpdateRule should validate null payload")
  void testCreateRuleNullPayload() {
    SlaPauseService service = new SlaPauseService(null, null);
    RestException ex = assertThrows(RestException.class, () -> service.createOrUpdateRule(null));
    assertEquals("Create SLA pause rule payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("createOrUpdateRule should validate null status")
  void testCreateRuleNullStatus() {
    SlaPauseService service = new SlaPauseService(null, null);
    CreateSlaPauseRuleRequest req = CreateSlaPauseRuleRequest.builder().build();
    RestException ex = assertThrows(RestException.class, () -> service.createOrUpdateRule(req));
    assertEquals("Ticket status is required for pause rule", ex.getMessage());
  }

  @Test
  @DisplayName("createOrUpdateRule should succeed and return rule response")
  void testCreateRuleSuccess() {
    SlaPauseRuleRepository dummyRepo =
        new DummySlaPauseRuleRepository() {
          @Override
          public Optional<SlaPauseRule> findByStatus(TicketStatus status) {
            return Optional.empty();
          }

          @Override
          public <S extends SlaPauseRule> S save(S entity) {
            entity.setId(1L);
            return entity;
          }
        };

    SlaPauseService service = new SlaPauseService(dummyRepo, null);
    CreateSlaPauseRuleRequest req =
        CreateSlaPauseRuleRequest.builder()
            .status(TicketStatus.PENDING_CUSTOMER)
            .pausesResolutionSla(true)
            .pausesResponseSla(false)
            .build();

    SlaPauseRuleResponse response = service.createOrUpdateRule(req);
    assertNotNull(response);
    assertEquals(TicketStatus.PENDING_CUSTOMER, response.getStatus());
    assertTrue(response.getPausesResolutionSla());
    assertFalse(response.getPausesResponseSla());
  }

  @Test
  @DisplayName("handleStatusTransition should pause ticket on pausing status")
  void testHandleStatusTransitionPause() {
    List<TicketSlaPauseInterval> savedIntervals = new ArrayList<>();

    SlaPauseRule rule =
        SlaPauseRule.builder()
            .status(TicketStatus.PENDING_CUSTOMER)
            .pausesResolutionSla(true)
            .isActive(true)
            .build();

    SlaPauseRuleRepository dummyRuleRepo =
        new DummySlaPauseRuleRepository() {
          @Override
          public Optional<SlaPauseRule> findByStatusAndIsActiveTrue(TicketStatus status) {
            if (status == TicketStatus.PENDING_CUSTOMER) {
              return Optional.of(rule);
            }
            return Optional.empty();
          }
        };

    TicketSlaPauseIntervalRepository dummyIntervalRepo =
        new DummyTicketSlaPauseIntervalRepository() {
          @Override
          public <S extends TicketSlaPauseInterval> S save(S entity) {
            savedIntervals.add(entity);
            return entity;
          }

          @Override
          public Optional<TicketSlaPauseInterval>
              findFirstByTicketIdAndResumedAtIsNullOrderByPausedAtDesc(Long ticketId) {
            return Optional.empty();
          }
        };

    SlaPauseService service = new SlaPauseService(dummyRuleRepo, dummyIntervalRepo);

    Ticket ticket =
        Ticket.builder()
            .id(100L)
            .ticketNumber("TKT-10001")
            .status(TicketStatus.IN_PROGRESS)
            .priority(TicketPriority.MEDIUM)
            .isSlaPaused(false)
            .build();

    User agent = User.builder().id(2L).username("agent1").build();

    service.handleStatusTransition(
        ticket, TicketStatus.IN_PROGRESS, TicketStatus.PENDING_CUSTOMER, agent, "Waiting for user details");

    assertTrue(ticket.getIsSlaPaused());
    assertNotNull(ticket.getSlaLastPausedAt());
    assertEquals(1, savedIntervals.size());
    assertEquals(TicketStatus.PENDING_CUSTOMER, savedIntervals.getFirst().getStatus());
    assertEquals("Waiting for user details", savedIntervals.getFirst().getPauseReason());
  }

  private static class DummySlaPauseRuleRepository implements SlaPauseRuleRepository {
    @Override public Optional<SlaPauseRule> findByStatusAndIsActiveTrue(TicketStatus status) { return Optional.empty(); }
    @Override public Optional<SlaPauseRule> findByStatus(TicketStatus status) { return Optional.empty(); }
    @Override public List<SlaPauseRule> findAllByIsActiveTrue() { return List.of(); }
    @Override public void flush() {}
    @Override public <S extends SlaPauseRule> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends SlaPauseRule> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<SlaPauseRule> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public SlaPauseRule getOne(Long aLong) { return null; }
    @Override public SlaPauseRule getById(Long aLong) { return null; }
    @Override public SlaPauseRule getReferenceById(Long aLong) { return null; }
    @Override public <S extends SlaPauseRule> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
    @Override public <S extends SlaPauseRule> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
    @Override public <S extends SlaPauseRule> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public <S extends SlaPauseRule> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    @Override public <S extends SlaPauseRule> long count(org.springframework.data.domain.Example<S> example) { return 0; }
    @Override public <S extends SlaPauseRule> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
    @Override public <S extends SlaPauseRule, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends SlaPauseRule> S save(S entity) { return entity; }
    @Override public <S extends SlaPauseRule> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public Optional<SlaPauseRule> findById(Long aLong) { return Optional.empty(); }
    @Override public boolean existsById(Long aLong) { return false; }
    @Override public List<SlaPauseRule> findAll() { return List.of(); }
    @Override public List<SlaPauseRule> findAllById(Iterable<Long> longs) { return List.of(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long aLong) {}
    @Override public void delete(SlaPauseRule entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends SlaPauseRule> entities) {}
    @Override public void deleteAll() {}
    @Override public List<SlaPauseRule> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public org.springframework.data.domain.Page<SlaPauseRule> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
  }

  private static class DummyTicketSlaPauseIntervalRepository implements TicketSlaPauseIntervalRepository {
    @Override public List<TicketSlaPauseInterval> findByTicketIdOrderByPausedAtAsc(Long ticketId) { return List.of(); }
    @Override public Optional<TicketSlaPauseInterval> findFirstByTicketIdAndResumedAtIsNullOrderByPausedAtDesc(Long ticketId) { return Optional.empty(); }
    @Override public void flush() {}
    @Override public <S extends TicketSlaPauseInterval> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends TicketSlaPauseInterval> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<TicketSlaPauseInterval> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public TicketSlaPauseInterval getOne(Long aLong) { return null; }
    @Override public TicketSlaPauseInterval getById(Long aLong) { return null; }
    @Override public TicketSlaPauseInterval getReferenceById(Long aLong) { return null; }
    @Override public <S extends TicketSlaPauseInterval> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
    @Override public <S extends TicketSlaPauseInterval> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
    @Override public <S extends TicketSlaPauseInterval> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public <S extends TicketSlaPauseInterval> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    @Override public <S extends TicketSlaPauseInterval> long count(org.springframework.data.domain.Example<S> example) { return 0; }
    @Override public <S extends TicketSlaPauseInterval> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
    @Override public <S extends TicketSlaPauseInterval, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends TicketSlaPauseInterval> S save(S entity) { return entity; }
    @Override public <S extends TicketSlaPauseInterval> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public Optional<TicketSlaPauseInterval> findById(Long aLong) { return Optional.empty(); }
    @Override public boolean existsById(Long aLong) { return false; }
    @Override public List<TicketSlaPauseInterval> findAll() { return List.of(); }
    @Override public List<TicketSlaPauseInterval> findAllById(Iterable<Long> longs) { return List.of(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long aLong) {}
    @Override public void delete(TicketSlaPauseInterval entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends TicketSlaPauseInterval> entities) {}
    @Override public void deleteAll() {}
    @Override public List<TicketSlaPauseInterval> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public org.springframework.data.domain.Page<TicketSlaPauseInterval> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
  }
}
