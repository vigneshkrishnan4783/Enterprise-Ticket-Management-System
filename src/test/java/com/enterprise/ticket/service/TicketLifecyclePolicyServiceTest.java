package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Organization;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketLifecyclePolicy;
import com.enterprise.ticket.model.TicketStatusHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.OrganizationRepository;
import com.enterprise.ticket.repository.TicketLifecyclePolicyRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketStatusHistoryRepository;
import com.enterprise.ticket.request.TicketLifecyclePolicyRequest;
import com.enterprise.ticket.response.TicketLifecyclePolicyResponse;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TicketLifecyclePolicyServiceTest {

  private TicketLifecyclePolicyService policyService;
  private List<TicketLifecyclePolicy> policyStore;
  private List<Organization> orgStore;
  private List<Ticket> ticketStore;
  private List<TicketStatusHistory> historyStore;
  private Organization acmeOrg;
  private User customerUser;

  @BeforeEach
  void setUp() {
    policyStore = new ArrayList<>();
    orgStore = new ArrayList<>();
    ticketStore = new ArrayList<>();
    historyStore = new ArrayList<>();

    acmeOrg = Organization.builder().id(10L).name("Acme Corp").build();
    orgStore.add(acmeOrg);

    customerUser =
        User.builder()
            .username("cust1")
            .email("cust1@acme.com")
            .userType(UserType.EXTERNAL_CUSTOMER)
            .build();
    customerUser.setId(50L);

    policyStore.add(
        TicketLifecyclePolicy.builder()
            .id(1L)
            .policyName("DEFAULT_POLICY")
            .allowCustomerReopen(true)
            .reopenWindowHours(72)
            .maxReopenCount(3)
            .autoCloseEnabled(true)
            .autoCloseAfterHours(72)
            .isActive(true)
            .build());

    policyService =
        new TicketLifecyclePolicyService(
            createPolicyRepo(policyStore),
            createOrgRepo(orgStore),
            createTicketRepo(ticketStore),
            createHistoryRepo(historyStore),
            null);
  }

  @Test
  @DisplayName("createOrUpdatePolicy should support organization override and global fallback")
  void testCreateOrUpdatePolicyGlobalAndOrgOverride() {
    TicketLifecyclePolicyRequest request =
        TicketLifecyclePolicyRequest.builder()
            .policyName("ACME_POLICY")
            .organizationId(10L)
            .allowCustomerReopen(false)
            .reopenWindowHours(24)
            .maxReopenCount(1)
            .autoCloseEnabled(true)
            .autoCloseAfterHours(48)
            .isActive(true)
            .build();

    TicketLifecyclePolicyResponse saved = policyService.createOrUpdatePolicy(request);
    assertEquals("ACME_POLICY", saved.getPolicyName());
    assertEquals(10L, saved.getOrganizationId());

    TicketLifecyclePolicy effectiveForAcme = policyService.getEffectivePolicy(acmeOrg);
    assertEquals("ACME_POLICY", effectiveForAcme.getPolicyName());
    assertEquals(false, effectiveForAcme.getAllowCustomerReopen());

    Organization otherOrg = Organization.builder().id(99L).name("Other").build();
    TicketLifecyclePolicy effectiveForOther = policyService.getEffectivePolicy(otherOrg);
    assertEquals("DEFAULT_POLICY", effectiveForOther.getPolicyName());
  }

  @Test
  @DisplayName("validateAndRecordReopen should allow customer within window and limit")
  void testValidateAndRecordReopenAllowedForCustomer() {
    Ticket ticket =
        Ticket.builder()
            .ticketNumber("TKT-20001")
            .status(TicketStatus.CLOSED)
            .resolvedAt(Instant.now().minus(12, ChronoUnit.HOURS))
            .closedAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .reopenCount(0)
            .build();

    policyService.validateAndRecordReopen(ticket, customerUser);

    assertEquals(1, ticket.getReopenCount());
    assertNull(ticket.getClosedAt());
  }

  @Test
  @DisplayName("validateAndRecordReopen should reject customer when reopening is disabled")
  void testValidateAndRecordReopenRejectedWhenDisabled() {
    policyStore.getFirst().setAllowCustomerReopen(false);
    Ticket ticket =
        Ticket.builder()
            .ticketNumber("TKT-20002")
            .status(TicketStatus.RESOLVED)
            .resolvedAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .reopenCount(0)
            .build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> policyService.validateAndRecordReopen(ticket, customerUser));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    assertEquals(0, ticket.getReopenCount());
  }

  @Test
  @DisplayName("validateAndRecordReopen should reject customer when reopen window has expired")
  void testValidateAndRecordReopenRejectedWhenWindowExpired() {
    Ticket ticket =
        Ticket.builder()
            .ticketNumber("TKT-20003")
            .status(TicketStatus.RESOLVED)
            .resolvedAt(Instant.now().minus(100, ChronoUnit.HOURS))
            .reopenCount(0)
            .build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> policyService.validateAndRecordReopen(ticket, customerUser));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("validateAndRecordReopen should reject customer when max reopen count reached")
  void testValidateAndRecordReopenRejectedWhenMaxCountReached() {
    Ticket ticket =
        Ticket.builder()
            .ticketNumber("TKT-20004")
            .status(TicketStatus.RESOLVED)
            .resolvedAt(Instant.now().minus(5, ChronoUnit.HOURS))
            .reopenCount(3)
            .build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> policyService.validateAndRecordReopen(ticket, customerUser));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("processAutoClosures should close only resolved tickets past policy window")
  void testProcessAutoClosuresClosesExpiredResolvedTickets() {
    Ticket expiredTicket =
        Ticket.builder()
            .ticketNumber("TKT-30001")
            .status(TicketStatus.RESOLVED)
            .resolvedAt(Instant.now().minus(80, ChronoUnit.HOURS))
            .reporter(customerUser)
            .build();
    expiredTicket.setId(1L);

    Ticket recentTicket =
        Ticket.builder()
            .ticketNumber("TKT-30002")
            .status(TicketStatus.RESOLVED)
            .resolvedAt(Instant.now().minus(10, ChronoUnit.HOURS))
            .reporter(customerUser)
            .build();
    recentTicket.setId(2L);

    ticketStore.add(expiredTicket);
    ticketStore.add(recentTicket);

    int closedCount = policyService.processAutoClosures();

    assertEquals(1, closedCount);
    assertEquals(TicketStatus.CLOSED, expiredTicket.getStatus());
    assertNotNull(expiredTicket.getClosedAt());
    assertEquals(TicketStatus.RESOLVED, recentTicket.getStatus());
    assertNull(recentTicket.getClosedAt());
    assertEquals(1, historyStore.size());
    assertEquals(TicketStatus.CLOSED, historyStore.getFirst().getNewStatus());
  }

  private static TicketLifecyclePolicyRepository createPolicyRepo(
      List<TicketLifecyclePolicy> store) {
    return (TicketLifecyclePolicyRepository)
        Proxy.newProxyInstance(
            TicketLifecyclePolicyRepository.class.getClassLoader(),
            new Class<?>[] {TicketLifecyclePolicyRepository.class},
            (proxy, method, args) -> {
              if ("findByPolicyNameIgnoreCase".equals(method.getName())) {
                String name = (String) args[0];
                return store.stream()
                    .filter(p -> name.equalsIgnoreCase(p.getPolicyName()))
                    .findFirst();
              }
              if ("findFirstByOrganizationIdAndIsActiveTrue".equals(method.getName())) {
                Long orgId = (Long) args[0];
                return store.stream()
                    .filter(
                        p ->
                            Boolean.TRUE.equals(p.getIsActive())
                                && p.getOrganization() != null
                                && orgId.equals(p.getOrganization().getId()))
                    .findFirst();
              }
              if ("findFirstByOrganizationIsNullAndIsActiveTrue".equals(method.getName())) {
                return store.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getOrganization() == null)
                    .findFirst();
              }
              if ("findAllByOrderByIdAsc".equals(method.getName())) {
                return new ArrayList<>(store);
              }
              if ("save".equals(method.getName())) {
                TicketLifecyclePolicy p = (TicketLifecyclePolicy) args[0];
                if (p.getId() == null) {
                  p.setId((long) (store.size() + 1));
                }
                store.removeIf(existing -> existing.getId().equals(p.getId()));
                store.add(p);
                return p;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static OrganizationRepository createOrgRepo(List<Organization> store) {
    return (OrganizationRepository)
        Proxy.newProxyInstance(
            OrganizationRepository.class.getClassLoader(),
            new Class<?>[] {OrganizationRepository.class},
            (proxy, method, args) -> {
              if ("findById".equals(method.getName())) {
                Long id = (Long) args[0];
                return store.stream().filter(o -> id.equals(o.getId())).findFirst();
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static TicketRepository createTicketRepo(List<Ticket> store) {
    return (TicketRepository)
        Proxy.newProxyInstance(
            TicketRepository.class.getClassLoader(),
            new Class<?>[] {TicketRepository.class},
            (proxy, method, args) -> {
              if ("findByStatus".equals(method.getName())) {
                TicketStatus status = (TicketStatus) args[0];
                return store.stream().filter(t -> status == t.getStatus()).toList();
              }
              if ("save".equals(method.getName())) {
                Ticket t = (Ticket) args[0];
                store.removeIf(existing -> existing.getId().equals(t.getId()));
                store.add(t);
                return t;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static TicketStatusHistoryRepository createHistoryRepo(List<TicketStatusHistory> store) {
    return (TicketStatusHistoryRepository)
        Proxy.newProxyInstance(
            TicketStatusHistoryRepository.class.getClassLoader(),
            new Class<?>[] {TicketStatusHistoryRepository.class},
            (proxy, method, args) -> {
              if ("save".equals(method.getName())) {
                TicketStatusHistory h = (TicketStatusHistory) args[0];
                h.setId((long) (store.size() + 1));
                store.add(h);
                return h;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }
}
