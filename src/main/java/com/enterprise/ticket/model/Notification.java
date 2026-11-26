package com.enterprise.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id", nullable = false)
  private User recipient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id")
  private Ticket ticket;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 50)
  private NotificationEventType eventType;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "is_read", nullable = false)
  @Builder.Default
  private Boolean isRead = false;

  @Column(name = "read_at")
  private Instant readAt;
}
