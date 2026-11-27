package com.enterprise.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_satisfaction_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSatisfactionFeedback extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false, unique = true)
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_id", nullable = false)
  private User submittedBy;

  @Column(name = "rating", nullable = false)
  private Integer rating;

  @Column(name = "feedback_comment", columnDefinition = "TEXT")
  private String feedbackComment;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @PrePersist
  public void prePersistFeedback() {
    if (submittedAt == null) {
      submittedAt = Instant.now();
    }
  }
}
