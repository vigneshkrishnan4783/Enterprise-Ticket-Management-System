package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enterprise.ticket.model.BusinessCalendar;
import com.enterprise.ticket.model.Holiday;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlaCalculatorServiceTest {

  @Test
  @DisplayName("calculateTargetInstant should use elapsed minutes when calendar is null")
  void testCalculateElapsedWhenCalendarNull() {
    SlaCalculatorService calculator = new SlaCalculatorService(null, null);
    Instant start = Instant.parse("2026-11-27T10:00:00Z");
    Instant target = calculator.calculateTargetInstant(start, 60, null, List.of());
    assertEquals(Instant.parse("2026-11-27T11:00:00Z"), target);
  }

  @Test
  @DisplayName("calculateTargetInstant should add minutes within business hours")
  void testCalculateWithinBusinessHours() {
    SlaCalculatorService calculator = new SlaCalculatorService(null, null);
    BusinessCalendar calendar =
        BusinessCalendar.builder()
            .timeZone("UTC")
            .workStartTime(LocalTime.of(9, 0))
            .workEndTime(LocalTime.of(18, 0))
            .workDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
            .isActive(true)
            .build();

    Instant start = Instant.parse("2026-11-27T10:00:00Z");
    Instant target = calculator.calculateTargetInstant(start, 120, calendar, List.of());
    assertEquals(Instant.parse("2026-11-27T12:00:00Z"), target);
  }

  @Test
  @DisplayName("calculateTargetInstant should skip weekend and holiday")
  void testCalculateSkipWeekendAndHoliday() {
    SlaCalculatorService calculator = new SlaCalculatorService(null, null);
    BusinessCalendar calendar =
        BusinessCalendar.builder()
            .timeZone("UTC")
            .workStartTime(LocalTime.of(9, 0))
            .workEndTime(LocalTime.of(17, 0))
            .workDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
            .isActive(true)
            .build();

    Instant fridayFourPm = Instant.parse("2026-11-27T16:00:00Z");

    Holiday mondayHoliday =
        Holiday.builder()
            .holidayDate(LocalDate.of(2026, 11, 30))
            .name("St Andrew Day")
            .build();

    Instant target =
        calculator.calculateTargetInstant(fridayFourPm, 120, calendar, List.of(mondayHoliday));

    ZonedDateTime zdt = target.atZone(ZoneId.of("UTC"));
    assertEquals(LocalDate.of(2026, 12, 1), zdt.toLocalDate());
    assertEquals(LocalTime.of(10, 0), zdt.toLocalTime());
  }
}
