package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.EscalationRule;
import com.enterprise.ticket.model.EscalationTriggerType;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketEscalationHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.EscalationRuleRepository;
import com.enterprise.ticket.repository.TeamRepository;
import com.enterprise.ticket.repository.TicketEscalationHistoryRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateEscalationRuleRequest;
import com.enterprise.ticket.request.EscalateTicketRequest;
import com.enterprise.ticket.response.EscalationRuleResponse;
import com.enterprise.ticket.response.TicketEscalationHistoryResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

class EscalationServiceTest {

  private EscalationService escalationService;
  private StubEscalationRuleRepository ruleRepository;
  private StubTicketEscalationHistoryRepository historyRepository;
  private StubTicketRepository ticketRepository;
  private StubUserRepository userRepository;
  private StubTeamRepository teamRepository;
  private User adminUser;
  private Ticket testTicket;

  @BeforeEach
  void setUp() {
    ruleRepository = new StubEscalationRuleRepository();
    historyRepository = new StubTicketEscalationHistoryRepository();
    ticketRepository = new StubTicketRepository();
    userRepository = new StubUserRepository();
    teamRepository = new StubTeamRepository();

    escalationService =
        new EscalationService(
            ruleRepository, historyRepository, ticketRepository, userRepository, teamRepository, null);

    Role adminRole = Role.builder().name(RoleType.ROLE_ADMIN).build();
    adminUser =
        User.builder()
            .username("admin")
            .roles(new HashSet<>(Set.of(adminRole)))
            .build();
    adminUser.setId(1L);
    userRepository.users.add(adminUser);

    testTicket =
        Ticket.builder()
            .ticketNumber("TKT-10001")
            .title("Server Down")
            .reporter(adminUser)
            .escalationLevel(0)
            .isEscalated(false)
            .build();
    testTicket.setId(10L);
    ticketRepository.tickets.add(testTicket);
  }

  @Test
  @DisplayName("createRule should validate empty name")
  void testCreateRuleEmptyName() {
    CreateEscalationRuleRequest req =
        CreateEscalationRuleRequest.builder()
            .name("   ")
            .triggerType(EscalationTriggerType.MANUAL)
            .build();
    assertThrows(RestException.class, () -> escalationService.createRule(req));
  }

  @Test
  @DisplayName("createRule should save and return rule response")
  void testCreateRuleSuccess() {
    CreateEscalationRuleRequest req =
        CreateEscalationRuleRequest.builder()
            .name("SLA Warning Rule")
            .triggerType(EscalationTriggerType.SLA_BREACH_WARNING)
            .priority(TicketPriority.CRITICAL)
            .targetLevel(2)
            .build();

    EscalationRuleResponse response = escalationService.createRule(req);
    assertNotNull(response);
    assertEquals("SLA Warning Rule", response.name());
    assertEquals(2, response.targetLevel());
    assertEquals(1, ruleRepository.rules.size());
  }

  @Test
  @DisplayName("getActiveRules should return active rules list")
  void testGetActiveRules() {
    EscalationRule r1 =
        EscalationRule.builder()
            .id(1L)
            .name("Rule 1")
            .triggerType(EscalationTriggerType.MANUAL)
            .targetLevel(1)
            .isActive(true)
            .build();
    ruleRepository.rules.add(r1);

    List<EscalationRuleResponse> rules = escalationService.getActiveRules();
    assertEquals(1, rules.size());
    assertEquals("Rule 1", rules.getFirst().name());
  }

  @Test
  @DisplayName("escalateTicket should increase escalationLevel and record history")
  void testEscalateTicketLevelBump() {
    EscalateTicketRequest req =
        EscalateTicketRequest.builder()
            .reason("Customer complaint about response delay")
            .targetLevel(1)
            .build();

    TicketEscalationHistoryResponse response =
        escalationService.escalateTicket("TKT-10001", req, "admin");

    assertNotNull(response);
    assertEquals(0, response.fromLevel());
    assertEquals(1, response.toLevel());
    assertEquals("Customer complaint about response delay", response.reason());
    assertEquals(1, testTicket.getEscalationLevel());
    assertTrue(testTicket.getIsEscalated());
    assertEquals(1, historyRepository.histories.size());
  }

  @Test
  @DisplayName("escalateTicket should reassign target team if specified")
  void testEscalateTicketWithTeamReassignment() {
    Team tier2Team = Team.builder().name("Tier 2 Support").build();
    tier2Team.setId(5L);
    teamRepository.teams.add(tier2Team);

    EscalateTicketRequest req =
        EscalateTicketRequest.builder()
            .reason("Requires advanced engineering investigation")
            .targetLevel(2)
            .targetTeamId(5L)
            .build();

    TicketEscalationHistoryResponse response =
        escalationService.escalateTicket("TKT-10001", req, "admin");

    assertNotNull(response);
    assertEquals("Tier 2 Support", response.newTeamName());
    assertEquals(tier2Team, testTicket.getAssignedTeam());
  }

  @Test
  @DisplayName("escalateTicket should validate empty reason")
  void testEscalateTicketEmptyReason() {
    EscalateTicketRequest req = EscalateTicketRequest.builder().reason("   ").build();
    assertThrows(RestException.class, () -> escalationService.escalateTicket("TKT-10001", req, "admin"));
  }

  @Test
  @DisplayName("getEscalationHistory should return chronological escalation records")
  void testGetEscalationHistory() {
    TicketEscalationHistory h =
        TicketEscalationHistory.builder()
            .id(1L)
            .ticket(testTicket)
            .fromLevel(0)
            .toLevel(1)
            .reason("SLA resolution breach")
            .escalatedBy(adminUser)
            .createdAt(Instant.now())
            .build();
    historyRepository.histories.add(h);

    List<TicketEscalationHistoryResponse> history =
        escalationService.getEscalationHistory("TKT-10001", "admin");

    assertEquals(1, history.size());
    assertEquals("SLA resolution breach", history.getFirst().reason());
    assertEquals("admin", history.getFirst().escalatedByUsername());
  }

  private static class StubEscalationRuleRepository implements EscalationRuleRepository {
    List<EscalationRule> rules = new ArrayList<>();
    AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<EscalationRule> findByIsActiveTrue() {
      return rules.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).toList();
    }

    @Override
    public List<EscalationRule> findByIsActiveTrueAndTriggerType(EscalationTriggerType triggerType) {
      return rules.stream()
          .filter(r -> Boolean.TRUE.equals(r.getIsActive()) && r.getTriggerType() == triggerType)
          .toList();
    }

    @Override
    public <S extends EscalationRule> S save(S entity) {
      if (entity.getId() == null) {
        entity.setId(idGenerator.getAndIncrement());
      }
      rules.removeIf(r -> r.getId().equals(entity.getId()));
      rules.add(entity);
      return entity;
    }

    @Override
    public Optional<EscalationRule> findById(Long id) {
      return rules.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    @Override public void flush() {}
    @Override public <S extends EscalationRule> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends EscalationRule> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<EscalationRule> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public EscalationRule getOne(Long id) { return null; }
    @Override public EscalationRule getById(Long id) { return null; }
    @Override public EscalationRule getReferenceById(Long id) { return null; }
    @Override public <S extends EscalationRule> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends EscalationRule> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends EscalationRule> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends EscalationRule> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends EscalationRule> long count(Example<S> example) { return 0; }
    @Override public <S extends EscalationRule> boolean exists(Example<S> example) { return false; }
    @Override public <S extends EscalationRule, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends EscalationRule> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<EscalationRule> findAll() { return rules; }
    @Override public List<EscalationRule> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return rules.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(EscalationRule entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends EscalationRule> entities) {}
    @Override public void deleteAll() {}
    @Override public List<EscalationRule> findAll(Sort sort) { return List.of(); }
    @Override public Page<EscalationRule> findAll(Pageable pageable) { return Page.empty(); }
  }

  private static class StubTicketEscalationHistoryRepository implements TicketEscalationHistoryRepository {
    List<TicketEscalationHistory> histories = new ArrayList<>();
    AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<TicketEscalationHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId) {
      return histories.stream().filter(h -> h.getTicket().getId().equals(ticketId)).toList();
    }

    @Override
    public <S extends TicketEscalationHistory> S save(S entity) {
      if (entity.getId() == null) {
        entity.setId(idGen.getAndIncrement());
      }
      histories.add(entity);
      return entity;
    }

    @Override public Optional<TicketEscalationHistory> findById(Long id) { return Optional.empty(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<TicketEscalationHistory> findAll() { return histories; }
    @Override public List<TicketEscalationHistory> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return histories.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(TicketEscalationHistory entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends TicketEscalationHistory> entities) {}
    @Override public void deleteAll() {}
    @Override public void flush() {}
    @Override public <S extends TicketEscalationHistory> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends TicketEscalationHistory> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<TicketEscalationHistory> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public TicketEscalationHistory getOne(Long id) { return null; }
    @Override public TicketEscalationHistory getById(Long id) { return null; }
    @Override public TicketEscalationHistory getReferenceById(Long id) { return null; }
    @Override public <S extends TicketEscalationHistory> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends TicketEscalationHistory> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends TicketEscalationHistory> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends TicketEscalationHistory> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends TicketEscalationHistory> long count(Example<S> example) { return 0; }
    @Override public <S extends TicketEscalationHistory> boolean exists(Example<S> example) { return false; }
    @Override public <S extends TicketEscalationHistory, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends TicketEscalationHistory> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public List<TicketEscalationHistory> findAll(Sort sort) { return List.of(); }
    @Override public Page<TicketEscalationHistory> findAll(Pageable pageable) { return Page.empty(); }
  }

  private static class StubTicketRepository implements TicketRepository {
    List<Ticket> tickets = new ArrayList<>();

    @Override
    public Optional<Ticket> findByTicketNumber(String ticketNumber) {
      return tickets.stream().filter(t -> t.getTicketNumber().equals(ticketNumber)).findFirst();
    }

    @Override
    public List<Ticket> findByStatus(com.enterprise.ticket.constant.TicketStatus status) {
      return tickets.stream().filter(t -> status == t.getStatus()).toList();
    }

    @Override
    public <S extends Ticket> S save(S entity) {
      return entity;
    }

    @Override public Optional<Ticket> findById(Long id) { return tickets.stream().filter(t -> t.getId().equals(id)).findFirst(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<Ticket> findAll() { return tickets; }
    @Override public List<Ticket> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return tickets.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(Ticket entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends Ticket> entities) {}
    @Override public void deleteAll() {}
    @Override public void flush() {}
    @Override public <S extends Ticket> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends Ticket> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<Ticket> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Ticket getOne(Long id) { return null; }
    @Override public Ticket getById(Long id) { return null; }
    @Override public Ticket getReferenceById(Long id) { return null; }
    @Override public <S extends Ticket> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Ticket> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends Ticket> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends Ticket> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Ticket> long count(Example<S> example) { return 0; }
    @Override public <S extends Ticket> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Ticket, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Ticket> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public List<Ticket> findAll(Sort sort) { return List.of(); }
    @Override public Page<Ticket> findAll(Pageable pageable) { return Page.empty(); }
    @Override public Optional<Ticket> findOne(org.springframework.data.jpa.domain.Specification<Ticket> spec) { return Optional.empty(); }
    @Override public List<Ticket> findAll(org.springframework.data.jpa.domain.Specification<Ticket> spec) { return List.of(); }
    @Override public Page<Ticket> findAll(org.springframework.data.jpa.domain.Specification<Ticket> spec, Pageable pageable) { return Page.empty(); }
    @Override public List<Ticket> findAll(org.springframework.data.jpa.domain.Specification<Ticket> spec, Sort sort) { return List.of(); }
    @Override public long count(org.springframework.data.jpa.domain.Specification<Ticket> spec) { return 0; }
    @Override public boolean exists(org.springframework.data.jpa.domain.Specification<Ticket> spec) { return false; }
    @Override public long delete(org.springframework.data.jpa.domain.Specification<Ticket> spec) { return 0; }
    @Override public <S extends Ticket, R> R findBy(org.springframework.data.jpa.domain.Specification<Ticket> spec, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public Page<Ticket> findByReporterId(Long reporterId, Pageable pageable) { return Page.empty(); }
    @Override public Long getNextTicketSequence() { return 1L; }
  }

  private static class StubUserRepository implements UserRepository {
    List<User> users = new ArrayList<>();

    @Override
    public Optional<User> findByUsername(String username) {
      return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public Optional<User> findById(Long id) {
      return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
    @Override public boolean existsByUsername(String username) { return false; }
    @Override public boolean existsByEmail(String email) { return false; }
    @Override public List<User> findAll() { return users; }
    @Override public List<User> findAll(Sort sort) { return List.of(); }
    @Override public Page<User> findAll(Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> S save(S entity) { return entity; }
    @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<User> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return users.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(User entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends User> entities) {}
    @Override public void deleteAll() {}
    @Override public void flush() {}
    @Override public <S extends User> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<User> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public User getOne(Long id) { return null; }
    @Override public User getById(Long id) { return null; }
    @Override public User getReferenceById(Long id) { return null; }
    @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends User> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> long count(Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> example) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
  }

  private static class StubTeamRepository implements TeamRepository {
    List<Team> teams = new ArrayList<>();

    @Override
    public Optional<Team> findById(Long id) {
      return teams.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    @Override public Optional<Team> findByCode(String code) { return Optional.empty(); }
    @Override public boolean existsByCode(String code) { return false; }
    @Override public boolean existsByName(String name) { return false; }
    @Override public List<Team> findByIsActiveTrue() { return teams; }
    @Override public <S extends Team> S save(S entity) { return entity; }
    @Override public <S extends Team> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<Team> findAll() { return teams; }
    @Override public List<Team> findAllById(Iterable<Long> ids) { return List.of(); }
    @Override public long count() { return teams.size(); }
    @Override public void deleteById(Long id) {}
    @Override public void delete(Team entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends Team> entities) {}
    @Override public void deleteAll() {}
    @Override public void flush() {}
    @Override public <S extends Team> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends Team> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<Team> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Team getOne(Long id) { return null; }
    @Override public Team getById(Long id) { return null; }
    @Override public Team getReferenceById(Long id) { return null; }
    @Override public <S extends Team> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Team> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends Team> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends Team> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Team> long count(Example<S> example) { return 0; }
    @Override public <S extends Team> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Team, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<Team> findAll(Sort sort) { return List.of(); }
    @Override public Page<Team> findAll(Pageable pageable) { return Page.empty(); }
  }
}
