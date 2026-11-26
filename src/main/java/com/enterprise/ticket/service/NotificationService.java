package com.enterprise.ticket.service;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Notification;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.NotificationRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.NotificationResponse;
import com.enterprise.ticket.response.UnreadCountResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @Transactional
  public void createNotification(
      User recipient, Ticket ticket, NotificationEventType eventType, String title, String message) {
    if (recipient == null) {
      return;
    }
    Notification notification =
        Notification.builder()
            .recipient(recipient)
            .ticket(ticket)
            .eventType(eventType)
            .title(title)
            .message(message)
            .isRead(false)
            .build();
    notificationRepository.save(notification);
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> getUserNotifications(String username, boolean unreadOnly) {
    User user = findUser(username);
    List<Notification> list =
        unreadOnly
            ? notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(user.getId())
            : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
    return list.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public UnreadCountResponse getUnreadCount(String username) {
    User user = findUser(username);
    return new UnreadCountResponse(notificationRepository.countByRecipientIdAndIsReadFalse(user.getId()));
  }

  @Transactional
  public NotificationResponse markAsRead(Long id, String username) {
    User user = findUser(username);
    Notification notification =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> RestException.notFound("Notification not found with id: " + id));
    if (!notification.getRecipient().getId().equals(user.getId())) {
      throw RestException.forbidden("You do not have permission to view or modify this notification");
    }
    if (Boolean.FALSE.equals(notification.getIsRead())) {
      notification.setIsRead(true);
      notification.setReadAt(Instant.now());
      notification = notificationRepository.save(notification);
    }
    return toResponse(notification);
  }

  @Transactional
  public void markAllAsRead(String username) {
    User user = findUser(username);
    notificationRepository.markAllAsReadForRecipient(user.getId());
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> RestException.notFound("User not found: " + username));
  }

  private NotificationResponse toResponse(Notification n) {
    return NotificationResponse.builder()
        .id(n.getId())
        .ticketNumber(n.getTicket() != null ? n.getTicket().getTicketNumber() : null)
        .eventType(n.getEventType())
        .title(n.getTitle())
        .message(n.getMessage())
        .isRead(n.getIsRead())
        .readAt(n.getReadAt())
        .createdAt(n.getCreatedAt())
        .build();
  }
}
