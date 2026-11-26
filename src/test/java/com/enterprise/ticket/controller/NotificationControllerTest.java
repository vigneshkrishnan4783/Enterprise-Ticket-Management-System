package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.NotificationResponse;
import com.enterprise.ticket.response.UnreadCountResponse;
import com.enterprise.ticket.service.NotificationService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NotificationControllerTest {

  private final Principal dummyPrincipal = () -> "testuser";

  @Test
  @DisplayName("getUserNotifications should return 200 OK and notifications list")
  void testGetUserNotifications() {
    NotificationService dummyService =
        new NotificationService(null, null) {
          @Override
          public List<NotificationResponse> getUserNotifications(String username, boolean unreadOnly) {
            return List.of(
                NotificationResponse.builder()
                    .id(1L)
                    .ticketNumber("TKT-10001")
                    .eventType(NotificationEventType.TICKET_CREATED)
                    .title("Ticket Created")
                    .message("Created")
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build());
          }
        };

    NotificationController controller = new NotificationController(dummyService);
    ResponseEntity<ApiResponse<List<NotificationResponse>>> response =
        controller.getUserNotifications(false, dummyPrincipal);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals("TKT-10001", response.getBody().getData().getFirst().ticketNumber());
  }

  @Test
  @DisplayName("getUnreadCount should return 200 OK and count")
  void testGetUnreadCount() {
    NotificationService dummyService =
        new NotificationService(null, null) {
          @Override
          public UnreadCountResponse getUnreadCount(String username) {
            return new UnreadCountResponse(5L);
          }
        };

    NotificationController controller = new NotificationController(dummyService);
    ResponseEntity<ApiResponse<UnreadCountResponse>> response =
        controller.getUnreadCount(dummyPrincipal);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(5L, response.getBody().getData().unreadCount());
  }

  @Test
  @DisplayName("markAsRead should return 200 OK and updated notification")
  void testMarkAsRead() {
    NotificationService dummyService =
        new NotificationService(null, null) {
          @Override
          public NotificationResponse markAsRead(Long id, String username) {
            return NotificationResponse.builder()
                .id(id)
                .ticketNumber("TKT-10001")
                .eventType(NotificationEventType.TICKET_ASSIGNED)
                .isRead(true)
                .readAt(Instant.now())
                .build();
          }
        };

    NotificationController controller = new NotificationController(dummyService);
    ResponseEntity<ApiResponse<NotificationResponse>> response =
        controller.markAsRead(1L, dummyPrincipal);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(true, response.getBody().getData().isRead());
  }

  @Test
  @DisplayName("markAllAsRead should return 200 OK")
  void testMarkAllAsRead() {
    NotificationService dummyService =
        new NotificationService(null, null) {
          @Override
          public void markAllAsRead(String username) {}
        };

    NotificationController controller = new NotificationController(dummyService);
    ResponseEntity<ApiResponse<Void>> response = controller.markAllAsRead(dummyPrincipal);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
