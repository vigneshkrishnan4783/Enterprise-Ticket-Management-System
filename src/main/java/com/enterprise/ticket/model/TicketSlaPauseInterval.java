package com.enterprise.ticket.model;

import com.enterprise.ticket.constant.TicketStatus;
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
@Table(name = "ticket_sla_pause_intervals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSlaPauseInterval {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private TicketStatus status;

  @Column(name = "paused_at", nullable = false)
  private Instant pausedAt;

  @Column(name = "resumed_at")
  private Instant resumedAt;

  @Column(name = "duration_minutes")
  private Long durationMinutes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "paused_by_id")
  private User pausedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resumed_by_id")
  private User resumedBy;

  @Column(name = "pause_reason", columnDefinition = "TEXT")
  private String pauseReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
