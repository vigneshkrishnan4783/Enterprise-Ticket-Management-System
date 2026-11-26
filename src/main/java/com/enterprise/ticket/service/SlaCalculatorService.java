package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.BusinessCalendar;
import com.enterprise.ticket.model.Holiday;
import com.enterprise.ticket.model.SlaPolicy;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.repository.SlaPolicyRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlaCalculatorService {

  private final SlaPolicyRepository slaPolicyRepository;
  private final BusinessCalendarService businessCalendarService;

  public void calculateDeadlines(Ticket ticket) {
    if (ticket == null || ticket.getPriority() == null) {
      return;
    }

    Instant baseline = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now();

    int responseMinutes;
    int resolutionMinutes;

    Optional<SlaPolicy> policyOpt =
        slaPolicyRepository.findByPriorityAndIsActiveTrue(ticket.getPriority());

    if (policyOpt.isPresent()) {
      SlaPolicy policy = policyOpt.get();
      responseMinutes = policy.getResponseTimeMinutes();
      resolutionMinutes = policy.getResolutionTimeMinutes();
    } else {
      responseMinutes = getDefaultResponseMinutes(ticket.getPriority());
      resolutionMinutes = getDefaultResolutionMinutes(ticket.getPriority());
    }

    BusinessCalendar calendar = businessCalendarService != null ? businessCalendarService.getDefaultCalendarEntity() : null;
    List<Holiday> holidays = calendar != null ? businessCalendarService.getHolidayEntities(calendar.getId()) : List.of();

    ticket.setResponseDueAt(calculateTargetInstant(baseline, responseMinutes, calendar, holidays));
    ticket.setResolutionDueAt(calculateTargetInstant(baseline, resolutionMinutes, calendar, holidays));
  }

  public void recalculateOnPriorityChange(Ticket ticket, TicketPriority newPriority) {
    if (ticket == null || newPriority == null) {
      return;
    }

    ticket.setPriority(newPriority);
    Instant now = Instant.now();

    int responseMinutes = getDefaultResponseMinutes(newPriority);
    int resolutionMinutes = getDefaultResolutionMinutes(newPriority);

    Optional<SlaPolicy> policyOpt = slaPolicyRepository.findByPriorityAndIsActiveTrue(newPriority);
    if (policyOpt.isPresent()) {
      responseMinutes = policyOpt.get().getResponseTimeMinutes();
      resolutionMinutes = policyOpt.get().getResolutionTimeMinutes();
    }

    BusinessCalendar calendar = businessCalendarService != null ? businessCalendarService.getDefaultCalendarEntity() : null;
    List<Holiday> holidays = calendar != null ? businessCalendarService.getHolidayEntities(calendar.getId()) : List.of();

    if (ticket.getFirstRespondedAt() == null) {
      ticket.setResponseDueAt(calculateTargetInstant(now, responseMinutes, calendar, holidays));
    }
    if (ticket.getResolvedAt() == null) {
      ticket.setResolutionDueAt(calculateTargetInstant(now, resolutionMinutes, calendar, holidays));
    }
  }

  public Instant calculateTargetInstant(
      Instant startInstant,
      int minutesToAdd,
      BusinessCalendar calendar,
      List<Holiday> holidays) {
    if (startInstant == null) {
      startInstant = Instant.now();
    }
    if (minutesToAdd <= 0) {
      return startInstant;
    }

    if (calendar == null || Boolean.FALSE.equals(calendar.getIsActive())) {
      return startInstant.plus(minutesToAdd, ChronoUnit.MINUTES);
    }

    LocalTime workStart = calendar.getWorkStartTime();
    LocalTime workEnd = calendar.getWorkEndTime();

    if (workStart == null || workEnd == null || !workEnd.isAfter(workStart)) {
      return startInstant.plus(minutesToAdd, ChronoUnit.MINUTES);
    }

    ZoneId zoneId;
    try {
      zoneId = ZoneId.of(calendar.getTimeZone());
    } catch (Exception _) {
      zoneId = ZoneId.of("UTC");
    }

    Set<DayOfWeek> workDays = new HashSet<>();
    if (calendar.getWorkDays() != null) {
      Arrays.stream(calendar.getWorkDays().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(s -> {
            try {
              workDays.add(DayOfWeek.valueOf(s.toUpperCase()));
            } catch (Exception _) {
            }
          });
    }
    if (workDays.isEmpty()) {
      workDays.addAll(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
    }

    Set<LocalDate> holidayDates =
        holidays != null
            ? new HashSet<>(holidays.stream().map(Holiday::getHolidayDate).toList())
            : Set.of();

    ZonedDateTime current = startInstant.atZone(zoneId);
    int remainingMinutes = minutesToAdd;

    while (remainingMinutes > 0) {
      LocalDate currentDate = current.toLocalDate();
      boolean isWorkDay = workDays.contains(currentDate.getDayOfWeek());
      boolean isHoliday = holidayDates.contains(currentDate);

      if (!isWorkDay || isHoliday) {
        current = current.plusDays(1).with(workStart);
        continue;
      }

      if (current.toLocalTime().isBefore(workStart)) {
        current = current.with(workStart);
      }

      if (!current.toLocalTime().isBefore(workEnd)) {
        current = current.plusDays(1).with(workStart);
        continue;
      }

      long availableTodayMinutes = Duration.between(current.toLocalTime(), workEnd).toMinutes();

      if (remainingMinutes <= availableTodayMinutes) {
        current = current.plusMinutes(remainingMinutes);
        remainingMinutes = 0;
      } else {
        remainingMinutes -= (int) availableTodayMinutes;
        current = current.plusDays(1).with(workStart);
      }
    }

    return current.toInstant();
  }

  private int getDefaultResponseMinutes(TicketPriority priority) {
    return switch (priority) {
      case CRITICAL -> 15;
      case HIGH -> 60;
      case MEDIUM -> 240;
      case LOW -> 480;
    };
  }

  private int getDefaultResolutionMinutes(TicketPriority priority) {
    return switch (priority) {
      case CRITICAL -> 240;
      case HIGH -> 480;
      case MEDIUM -> 1440;
      case LOW -> 2880;
    };
  }
}
