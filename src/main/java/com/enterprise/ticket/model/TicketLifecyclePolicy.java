package com.enterprise.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ticket_lifecycle_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLifecyclePolicy extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "policy_name", nullable = false, unique = true, length = 100)
  private String policyName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "allow_customer_reopen", nullable = false)
  @Builder.Default
  private Boolean allowCustomerReopen = true;

  @Column(name = "reopen_window_hours", nullable = false)
  @Builder.Default
  private Integer reopenWindowHours = 72;

  @Column(name = "max_reopen_count", nullable = false)
  @Builder.Default
  private Integer maxReopenCount = 3;

  @Column(name = "auto_close_enabled", nullable = false)
  @Builder.Default
  private Boolean autoCloseEnabled = true;

  @Column(name = "auto_close_after_hours", nullable = false)
  @Builder.Default
  private Integer autoCloseAfterHours = 72;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
