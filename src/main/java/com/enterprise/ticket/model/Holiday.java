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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "holidays",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_calendar_holiday",
          columnNames = {"business_calendar_id", "holiday_date"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_calendar_id", nullable = false)
  private BusinessCalendar businessCalendar;

  @Column(name = "holiday_date", nullable = false)
  private LocalDate holidayDate;

  @Column(name = "name", nullable = false, length = 100)
  private String name;
}
