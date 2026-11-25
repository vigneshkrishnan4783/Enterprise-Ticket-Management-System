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
@Table(name = "ticket_assignment_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignmentHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_by_id")
  private User assignedBy;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "assignment_source", nullable = false, length = 50)
  @Builder.Default
  private com.enterprise.ticket.constant.AssignmentSource assignmentSource =
      com.enterprise.ticket.constant.AssignmentSource.MANUAL;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rule_id")
  private AssignmentRule rule;

  @Column(name = "comment", columnDefinition = "TEXT")
  private String comment;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
