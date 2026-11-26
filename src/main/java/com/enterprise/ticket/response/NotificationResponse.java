package com.enterprise.ticket.response;

import com.enterprise.ticket.model.NotificationEventType;
import java.time.Instant;
import lombok.Builder;

@Builder
public record NotificationResponse(
    Long id,
    String ticketNumber,
    NotificationEventType eventType,
    String title,
    String message,
    Boolean isRead,
    Instant readAt,
    Instant createdAt) {}
