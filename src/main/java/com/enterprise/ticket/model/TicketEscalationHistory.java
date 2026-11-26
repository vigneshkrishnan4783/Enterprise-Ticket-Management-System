package com.enterprise.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_escalation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEscalationHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rule_id")
  private EscalationRule rule;

  @Column(name = "from_level", nullable = false)
  private Integer fromLevel;

  @Column(name = "to_level", nullable = false)
  private Integer toLevel;

  @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "escalated_by")
  private User escalatedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_team_id")
  private Team previousTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "new_team_id")
  private Team newTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_agent_id")
  private User previousAgent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "new_agent_id")
  private User newAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }
}
