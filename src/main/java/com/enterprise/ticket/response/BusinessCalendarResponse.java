package com.enterprise.ticket.response;

import java.time.Instant;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCalendarResponse {

  private Long id;
  private String name;
  private String timeZone;
  private LocalTime workStartTime;
  private LocalTime workEndTime;
  private String workDays;
  private Boolean isDefault;
  private Boolean isActive;
  private Instant createdAt;
}
