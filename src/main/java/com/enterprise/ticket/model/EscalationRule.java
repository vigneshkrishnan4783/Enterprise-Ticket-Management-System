package com.enterprise.ticket.model;

import com.enterprise.ticket.constant.TicketPriority;
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
@Table(name = "escalation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationRule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 50)
  private EscalationTriggerType triggerType;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", length = 20)
  private TicketPriority priority;

  @Column(name = "target_level", nullable = false)
  @Builder.Default
  private Integer targetLevel = 1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_team_id")
  private Team targetTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_agent_id")
  private User targetAgent;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
