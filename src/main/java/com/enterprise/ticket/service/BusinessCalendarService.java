package com.enterprise.ticket.service;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.BusinessCalendar;
import com.enterprise.ticket.model.Holiday;
import com.enterprise.ticket.repository.BusinessCalendarRepository;
import com.enterprise.ticket.repository.HolidayRepository;
import com.enterprise.ticket.request.CreateBusinessCalendarRequest;
import com.enterprise.ticket.request.CreateHolidayRequest;
import com.enterprise.ticket.response.BusinessCalendarResponse;
import com.enterprise.ticket.response.HolidayResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessCalendarService {

  private final BusinessCalendarRepository calendarRepository;
  private final HolidayRepository holidayRepository;

  @Transactional
  public BusinessCalendarResponse createCalendar(CreateBusinessCalendarRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create business calendar payload cannot be empty");
    }
    if (request.getName() == null || request.getName().isBlank()) {
      throw RestException.badRequest("Calendar name is required");
    }

    if (calendarRepository.findByName(request.getName().trim()).isPresent()) {
      throw RestException.conflict("Business calendar name already exists: " + request.getName());
    }

    if (Boolean.TRUE.equals(request.getIsDefault())) {
      calendarRepository.findByIsDefaultTrueAndIsActiveTrue().ifPresent(c -> {
        c.setIsDefault(false);
        calendarRepository.save(c);
      });
    }

    BusinessCalendar calendar =
        BusinessCalendar.builder()
            .name(request.getName().trim())
            .timeZone(request.getTimeZone() != null ? request.getTimeZone().trim() : "UTC")
            .workStartTime(request.getWorkStartTime())
            .workEndTime(request.getWorkEndTime())
            .workDays(request.getWorkDays() != null ? request.getWorkDays().trim() : "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
            .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
            .isActive(true)
            .build();

    return toCalendarResponse(calendarRepository.save(calendar));
  }

  @Transactional(readOnly = true)
  public List<BusinessCalendarResponse> getAllActiveCalendars() {
    return calendarRepository.findAllByIsActiveTrue().stream()
        .map(this::toCalendarResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public BusinessCalendar getCalendarEntity(Long id) {
    return calendarRepository
        .findById(id)
        .orElseThrow(() -> RestException.notFound("Business calendar not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public BusinessCalendar getDefaultCalendarEntity() {
    return calendarRepository
        .findByIsDefaultTrueAndIsActiveTrue()
        .orElseGet(() -> calendarRepository.findAll().stream().findFirst().orElse(null));
  }

  @Transactional
  public HolidayResponse addHoliday(Long calendarId, CreateHolidayRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create holiday payload cannot be empty");
    }
    if (request.getHolidayDate() == null) {
      throw RestException.badRequest("Holiday date is required");
    }
    if (request.getName() == null || request.getName().isBlank()) {
      throw RestException.badRequest("Holiday name is required");
    }

    BusinessCalendar calendar = getCalendarEntity(calendarId);

    if (holidayRepository.existsByBusinessCalendarIdAndHolidayDate(calendarId, request.getHolidayDate())) {
      throw RestException.conflict("Holiday already exists on date: " + request.getHolidayDate());
    }

    Holiday holiday =
        Holiday.builder()
            .businessCalendar(calendar)
            .holidayDate(request.getHolidayDate())
            .name(request.getName().trim())
            .build();

    return toHolidayResponse(holidayRepository.save(holiday));
  }

  @Transactional(readOnly = true)
  public List<HolidayResponse> getHolidays(Long calendarId) {
    getCalendarEntity(calendarId);
    return holidayRepository.findByBusinessCalendarIdOrderByHolidayDateAsc(calendarId).stream()
        .map(this::toHolidayResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Holiday> getHolidayEntities(Long calendarId) {
    return holidayRepository.findByBusinessCalendarIdOrderByHolidayDateAsc(calendarId);
  }

  private BusinessCalendarResponse toCalendarResponse(BusinessCalendar c) {
    return BusinessCalendarResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .timeZone(c.getTimeZone())
        .workStartTime(c.getWorkStartTime())
        .workEndTime(c.getWorkEndTime())
        .workDays(c.getWorkDays())
        .isDefault(c.getIsDefault())
        .isActive(c.getIsActive())
        .createdAt(c.getCreatedAt())
        .build();
  }

  private HolidayResponse toHolidayResponse(Holiday h) {
    return HolidayResponse.builder()
        .id(h.getId())
        .businessCalendarId(h.getBusinessCalendar().getId())
        .holidayDate(h.getHolidayDate())
        .name(h.getName())
        .createdAt(h.getCreatedAt())
        .build();
  }
}
