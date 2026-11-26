package com.enterprise.ticket.response;

import java.time.Instant;
import java.time.LocalDate;
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
public class HolidayResponse {

  private Long id;
  private Long businessCalendarId;
  private LocalDate holidayDate;
  private String name;
  private Instant createdAt;
}
