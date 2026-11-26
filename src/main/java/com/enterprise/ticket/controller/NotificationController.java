package com.enterprise.ticket.controller;

import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.NotificationResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.UnreadCountResponse;
import com.enterprise.ticket.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for managing user notifications on ticket lifecycle events")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @Operation(summary = "Get user notifications", description = "Retrieves in-app notifications for the authenticated user")
  public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
      @RequestParam(defaultValue = "false") boolean unreadOnly, Principal principal) {
    return ResponseBuilder.<List<NotificationResponse>>builder()
        .success()
        .withData(notificationService.getUserNotifications(principal.getName(), unreadOnly))
        .build();
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Get unread notifications count", description = "Retrieves the count of unread notifications for badge display")
  public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Principal principal) {
    return ResponseBuilder.<UnreadCountResponse>builder()
        .success()
        .withData(notificationService.getUnreadCount(principal.getName()))
        .build();
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
  public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
      @PathVariable Long id, Principal principal) {
    return ResponseBuilder.<NotificationResponse>builder()
        .success("Notification marked as read")
        .withData(notificationService.markAsRead(id, principal.getName()))
        .build();
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for current user")
  public ResponseEntity<ApiResponse<Void>> markAllAsRead(Principal principal) {
    notificationService.markAllAsRead(principal.getName());
    return ResponseBuilder.<Void>builder()
        .success("All notifications marked as read")
        .build();
  }
}
