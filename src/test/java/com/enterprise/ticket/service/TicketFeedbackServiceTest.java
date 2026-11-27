package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketSatisfactionFeedback;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketSatisfactionFeedbackRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.SubmitFeedbackRequest;
import com.enterprise.ticket.response.FeedbackResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TicketFeedbackServiceTest {

  private TicketFeedbackService feedbackService;
  private List<TicketSatisfactionFeedback> feedbackStore;
  private List<Ticket> ticketStore;
  private List<User> userStore;
  private Ticket resolvedTicket;
  private User customerUser;
  private User agentUser;

  @BeforeEach
  void setUp() {
    feedbackStore = new ArrayList<>();
    ticketStore = new ArrayList<>();
    userStore = new ArrayList<>();

    Role customerRole = new Role();
    customerRole.setId(1L);
    customerRole.setName(RoleType.ROLE_CUSTOMER);

    Role agentRole = new Role();
    agentRole.setId(2L);
    agentRole.setName(RoleType.ROLE_AGENT);

    customerUser =
        User.builder()
            .username("kerala_traffic_client")
            .email("ops@keralatraffic.gov.in")
            .userType(UserType.EXTERNAL_CUSTOMER)
            .organizationName("Kerala Traffic Police")
            .roles(Set.of(customerRole))
            .build();
    customerUser.setId(10L);
    userStore.add(customerUser);

    agentUser =
        User.builder()
            .username("harish_dev")
            .email("harish@surveilai.com")
            .userType(UserType.INTERNAL)
            .roles(Set.of(agentRole))
            .build();
    agentUser.setId(20L);
    userStore.add(agentUser);

    resolvedTicket =
        Ticket.builder()
            .ticketNumber("TKT-10012")
            .title("Kochi Edappally Junction AI CCTV Offline")
            .status(TicketStatus.RESOLVED)
            .reporter(customerUser)
            .assignedAgent(agentUser)
            .build();
    resolvedTicket.setId(100L);
    ticketStore.add(resolvedTicket);

    feedbackService =
        new TicketFeedbackService(
            createFeedbackRepo(feedbackStore),
            createTicketRepo(ticketStore),
            createUserRepo(userStore),
            null);
  }

  @Test
  @DisplayName("submitFeedback should succeed for resolved ticket by requester")
  void testSubmitFeedbackSuccess() {
    SubmitFeedbackRequest request =
        SubmitFeedbackRequest.builder()
            .rating(5)
            .feedbackComment("  Quickly restored the ANPR camera stream!  ")
            .build();

    FeedbackResponse response =
        feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client");

    assertEquals("TKT-10012", response.getTicketNumber());
    assertEquals(5, response.getRating());
    assertEquals("Quickly restored the ANPR camera stream!", response.getFeedbackComment());
    assertEquals("kerala_traffic_client", response.getSubmittedByUsername());
    assertNotNull(response.getSubmittedAt());
    assertEquals(1, feedbackStore.size());
  }

  @Test
  @DisplayName("submitFeedback should reject feedback when ticket is not resolved or closed")
  void testSubmitFeedbackRejectedBeforeResolution() {
    resolvedTicket.setStatus(TicketStatus.IN_PROGRESS);
    SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().rating(4).build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    assertEquals(0, feedbackStore.size());
  }

  @Test
  @DisplayName("submitFeedback should reject feedback from non-requester user")
  void testSubmitFeedbackRejectedForNonRequester() {
    SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().rating(5).build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> feedbackService.submitFeedback("TKT-10012", request, "harish_dev"));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
  }

  @Test
  @DisplayName("submitFeedback should reject duplicate feedback for the same ticket")
  void testSubmitFeedbackRejectedWhenDuplicate() {
    SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().rating(5).build();
    feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client");

    RestException ex =
        assertThrows(
            RestException.class,
            () -> feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client"));

    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
  }

  @Test
  @DisplayName("submitFeedback should reject rating outside 1 to 5")
  void testSubmitFeedbackRejectedWhenRatingOutOfRange() {
    SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().rating(6).build();

    RestException ex =
        assertThrows(
            RestException.class,
            () -> feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("getFeedback should allow agent to view submitted feedback")
  void testGetFeedbackSuccessForAgent() {
    SubmitFeedbackRequest request =
        SubmitFeedbackRequest.builder().rating(4).feedbackComment("Good support").build();
    feedbackService.submitFeedback("TKT-10012", request, "kerala_traffic_client");

    FeedbackResponse response = feedbackService.getFeedback("TKT-10012", "harish_dev");

    assertEquals(4, response.getRating());
    assertEquals("Good support", response.getFeedbackComment());
  }

  private static TicketSatisfactionFeedbackRepository createFeedbackRepo(
      List<TicketSatisfactionFeedback> store) {
    return (TicketSatisfactionFeedbackRepository)
        Proxy.newProxyInstance(
            TicketSatisfactionFeedbackRepository.class.getClassLoader(),
            new Class<?>[] {TicketSatisfactionFeedbackRepository.class},
            (proxy, method, args) -> {
              if ("existsByTicketId".equals(method.getName())) {
                Long ticketId = (Long) args[0];
                return store.stream()
                    .anyMatch(f -> f.getTicket() != null && ticketId.equals(f.getTicket().getId()));
              }
              if ("findByTicketId".equals(method.getName())) {
                Long ticketId = (Long) args[0];
                return store.stream()
                    .filter(f -> f.getTicket() != null && ticketId.equals(f.getTicket().getId()))
                    .findFirst();
              }
              if ("save".equals(method.getName())) {
                TicketSatisfactionFeedback f = (TicketSatisfactionFeedback) args[0];
                if (f.getId() == null) {
                  f.setId((long) (store.size() + 1));
                }
                store.add(f);
                return f;
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
              if ("findByTicketNumber".equals(method.getName())) {
                String num = (String) args[0];
                return store.stream().filter(t -> num.equals(t.getTicketNumber())).findFirst();
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
}
