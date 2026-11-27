package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.constant.ResolutionCode;
import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketStatusHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketCommentRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketStatusHistoryRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.UpdateTicketStatusRequest;
import com.enterprise.ticket.response.TicketResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TicketTimelineServiceTest {

  private TicketTimelineService timelineService;
  private List<Ticket> ticketStore;
  private List<User> userStore;
  private List<TicketStatusHistory> historyStore;
  private User agentUser;
  private Ticket openTicket;

  @BeforeEach
  void setUp() {
    ticketStore = new ArrayList<>();
    userStore = new ArrayList<>();
    historyStore = new ArrayList<>();

    Role agentRole = new Role();
    agentRole.setId(1L);
    agentRole.setName(RoleType.ROLE_AGENT);

    agentUser =
        User.builder()
            .username("agent1")
            .email("agent1@example.com")
            .userType(UserType.INTERNAL)
            .roles(Set.of(agentRole))
            .build();
    agentUser.setId(10L);
    userStore.add(agentUser);

    User reporter =
        User.builder().username("customer1").email("cust@example.com").build();
    reporter.setId(20L);
    userStore.add(reporter);

    openTicket =
        Ticket.builder()
            .ticketNumber("TKT-10001")
            .title("VPN connection drops")
            .status(TicketStatus.IN_PROGRESS)
            .reporter(reporter)
            .assignedAgent(agentUser)
            .build();
    openTicket.setId(100L);
    ticketStore.add(openTicket);

    TicketRepository ticketRepository = createTicketRepo(ticketStore);
    UserRepository userRepository = createUserRepo(userStore);
    TicketCommentRepository commentRepository =
        (TicketCommentRepository)
            Proxy.newProxyInstance(
                TicketCommentRepository.class.getClassLoader(),
                new Class<?>[] {TicketCommentRepository.class},
                (proxy, method, args) -> {
                  throw new UnsupportedOperationException(method.getName());
                });
    TicketStatusHistoryRepository statusHistoryRepository = createHistoryRepo(historyStore);

    TicketService ticketServiceStub =
        new TicketService(
            ticketRepository,
            userRepository,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null) {
          @Override
          public TicketResponse getTicketByNumber(String ticketNumber, String username) {
            Ticket t =
                ticketStore.stream()
                    .filter(item -> ticketNumber.equals(item.getTicketNumber()))
                    .findFirst()
                    .orElseThrow();
            return TicketResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .status(t.getStatus())
                .resolvedAt(t.getResolvedAt())
                .resolutionCode(t.getResolutionCode())
                .resolutionNotes(t.getResolutionNotes())
                .resolvedByUsername(
                    t.getResolvedBy() != null ? t.getResolvedBy().getUsername() : null)
                .build();
          }
        };

    timelineService =
        new TicketTimelineService(
            ticketRepository,
            userRepository,
            commentRepository,
            statusHistoryRepository,
            ticketServiceStub,
            null,
            null);
  }

  @Test
  @DisplayName("updateStatus to RESOLVED should fail when resolutionNotes is null")
  void testUpdateStatusToResolvedFailsWhenResolutionNotesNull() {
    UpdateTicketStatusRequest request =
        UpdateTicketStatusRequest.builder()
            .status(TicketStatus.RESOLVED)
            .reason("Fixed")
            .build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> timelineService.updateStatus("TKT-10001", request, "agent1"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals(TicketStatus.IN_PROGRESS, openTicket.getStatus());
    assertNull(openTicket.getResolvedAt());
    assertEquals(0, historyStore.size());
  }

  @Test
  @DisplayName("updateStatus to RESOLVED should fail when resolutionNotes is blank")
  void testUpdateStatusToResolvedFailsWhenResolutionNotesBlank() {
    UpdateTicketStatusRequest request =
        UpdateTicketStatusRequest.builder()
            .status(TicketStatus.RESOLVED)
            .resolutionCode(ResolutionCode.FIXED)
            .resolutionNotes("   ")
            .build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> timelineService.updateStatus("TKT-10001", request, "agent1"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals(TicketStatus.IN_PROGRESS, openTicket.getStatus());
    assertNull(openTicket.getResolutionNotes());
  }

  @Test
  @DisplayName("updateStatus to RESOLVED should succeed and persist resolution details")
  void testUpdateStatusToResolvedSuccessWithDetails() {
    UpdateTicketStatusRequest request =
        UpdateTicketStatusRequest.builder()
            .status(TicketStatus.RESOLVED)
            .resolutionCode(ResolutionCode.FIXED)
            .resolutionNotes("  Restarted VPN gateway and verified tunnel stability.  ")
            .reason("Issue resolved by agent")
            .build();

    TicketResponse response = timelineService.updateStatus("TKT-10001", request, "agent1");

    assertEquals(TicketStatus.RESOLVED, response.getStatus());
    assertEquals(ResolutionCode.FIXED, response.getResolutionCode());
    assertEquals(
        "Restarted VPN gateway and verified tunnel stability.", response.getResolutionNotes());
    assertEquals("agent1", response.getResolvedByUsername());
    assertNotNull(response.getResolvedAt());
    assertEquals(1, historyStore.size());
    assertEquals(TicketStatus.RESOLVED, historyStore.getFirst().getNewStatus());
  }

  @Test
  @DisplayName("updateStatus to PENDING_CUSTOMER should succeed without resolution details")
  void testUpdateStatusToNonResolvedSucceedsWithoutResolutionDetails() {
    UpdateTicketStatusRequest request =
        UpdateTicketStatusRequest.builder()
            .status(TicketStatus.PENDING_CUSTOMER)
            .reason("Waiting for user logs")
            .build();

    TicketResponse response = timelineService.updateStatus("TKT-10001", request, "agent1");

    assertEquals(TicketStatus.PENDING_CUSTOMER, response.getStatus());
    assertNull(response.getResolutionNotes());
    assertNull(response.getResolvedAt());
    assertEquals(1, historyStore.size());
  }

  private static TicketRepository createTicketRepo(List<Ticket> store) {
    return (TicketRepository)
        Proxy.newProxyInstance(
            TicketRepository.class.getClassLoader(),
            new Class<?>[] {TicketRepository.class},
            (proxy, method, args) -> {
              if ("findByTicketNumber".equals(method.getName())) {
                String num = (String) args[0];
                return store.stream().filter(t -> num.equals(t.getTicketNumber())).findFirst();
              }
              if ("save".equals(method.getName())) {
                Ticket t = (Ticket) args[0];
                if (t.getId() == null) {
                  t.setId((long) (store.size() + 1));
                }
                store.removeIf(existing -> existing.getId().equals(t.getId()));
                store.add(t);
                return t;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static UserRepository createUserRepo(List<User> store) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              if ("findByUsername".equals(method.getName())) {
                String name = (String) args[0];
                return store.stream().filter(u -> name.equals(u.getUsername())).findFirst();
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
