package com.enterprise.ticket.model;

import com.enterprise.ticket.constant.TicketChannel;
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
@Table(name = "assignment_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 150)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "priority_order", nullable = false)
  @Builder.Default
  private Integer priorityOrder = 100;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subcategory_id")
  private Subcategory subcategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "site_id")
  private Site site;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "impact_id")
  private Impact impact;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "urgency_id")
  private Urgency urgency;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", length = 50)
  private TicketPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", length = 50)
  private TicketChannel channel;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_team_id", nullable = false)
  private Team targetTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_agent_id")
  private User targetAgent;

  @Column(name = "reason_template", length = 255)
  private String reasonTemplate;
}
