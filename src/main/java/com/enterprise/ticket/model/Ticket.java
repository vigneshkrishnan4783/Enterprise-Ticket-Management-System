package com.enterprise.ticket.model;

import com.enterprise.ticket.constant.TicketChannel;
import com.enterprise.ticket.constant.TicketPriority;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
  private String ticketNumber;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  @Builder.Default
  private TicketStatus status = TicketStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 50)
  @Builder.Default
  private TicketPriority priority = TicketPriority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 50)
  @Builder.Default
  private TicketChannel channel = TicketChannel.WEB;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "site_id")
  private Site site;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id")
  private Asset asset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subcategory_id")
  private Subcategory subcategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "impact_id")
  private Impact impact;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "urgency_id")
  private Urgency urgency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_team_id")
  private Team assignedTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_agent_id")
  private User assignedAgent;

  @Column(name = "assigned_at")
  private java.time.Instant assignedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "assignment_source", length = 50)
  private com.enterprise.ticket.constant.AssignmentSource assignmentSource;

  @Column(name = "assignment_reason", columnDefinition = "TEXT")
  private String assignmentReason;

  @Column(name = "response_due_at")
  private java.time.Instant responseDueAt;

  @Column(name = "resolution_due_at")
  private java.time.Instant resolutionDueAt;

  @Column(name = "first_responded_at")
  private java.time.Instant firstRespondedAt;

  @Column(name = "resolved_at")
  private java.time.Instant resolvedAt;

  @Column(name = "is_response_breached", nullable = false)
  @Builder.Default
  private Boolean isResponseBreached = false;

  @Column(name = "is_resolution_breached", nullable = false)
  @Builder.Default
  private Boolean isResolutionBreached = false;

  @Column(name = "total_paused_duration_minutes", nullable = false)
  @Builder.Default
  private Long totalPausedDurationMinutes = 0L;

  @Column(name = "is_sla_paused", nullable = false)
  @Builder.Default
  private Boolean isSlaPaused = false;

  @Column(name = "sla_last_paused_at")
  private java.time.Instant slaLastPausedAt;

  @Column(name = "escalation_level", nullable = false)
  @Builder.Default
  private Integer escalationLevel = 0;

  @Column(name = "is_escalated", nullable = false)
  @Builder.Default
  private Boolean isEscalated = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "resolution_code", length = 50)
  private com.enterprise.ticket.constant.ResolutionCode resolutionCode;

  @Column(name = "resolution_notes", columnDefinition = "TEXT")
  private String resolutionNotes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by_id")
  private User resolvedBy;

  @Column(name = "reopen_count", nullable = false)
  @Builder.Default
  private Integer reopenCount = 0;

  @Column(name = "closed_at")
  private java.time.Instant closedAt;
}
