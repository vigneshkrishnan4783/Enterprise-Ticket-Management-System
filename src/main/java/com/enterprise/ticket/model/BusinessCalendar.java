package com.enterprise.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "business_calendars")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCalendar extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "time_zone", nullable = false, length = 50)
  @Builder.Default
  private String timeZone = "UTC";

  @Column(name = "work_start_time", nullable = false)
  @Builder.Default
  private LocalTime workStartTime = LocalTime.of(9, 0);

  @Column(name = "work_end_time", nullable = false)
  @Builder.Default
  private LocalTime workEndTime = LocalTime.of(18, 0);

  @Column(name = "work_days", nullable = false, length = 100)
  @Builder.Default
  private String workDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";

  @Column(name = "is_default", nullable = false)
  @Builder.Default
  private Boolean isDefault = false;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;
}
