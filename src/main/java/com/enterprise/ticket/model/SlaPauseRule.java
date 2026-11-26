package com.enterprise.ticket.model;

import com.enterprise.ticket.constant.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sla_pause_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaPauseRule extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, unique = true, length = 50)
  private TicketStatus status;

  @Column(name = "pauses_response_sla", nullable = false)
  @Builder.Default
  private Boolean pausesResponseSla = false;

  @Column(name = "pauses_resolution_sla", nullable = false)
  @Builder.Default
  private Boolean pausesResolutionSla = true;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
