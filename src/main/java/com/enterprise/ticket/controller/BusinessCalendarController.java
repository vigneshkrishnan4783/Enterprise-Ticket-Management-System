package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateBusinessCalendarRequest;
import com.enterprise.ticket.request.CreateHolidayRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.BusinessCalendarResponse;
import com.enterprise.ticket.response.HolidayResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.BusinessCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-calendars")
@RequiredArgsConstructor
@Tag(name = "Business Calendars", description = "Endpoints for managing business hours, calendars, and holidays")
public class BusinessCalendarController {

  private final BusinessCalendarService businessCalendarService;

  @PostMapping
  @Operation(summary = "Create business calendar", description = "Creates a new business calendar with custom working hours and days")
  public ResponseEntity<ApiResponse<BusinessCalendarResponse>> createCalendar(
      @Valid @RequestBody CreateBusinessCalendarRequest request) {
    return ResponseBuilder.<BusinessCalendarResponse>builder()
        .created("Business calendar created successfully")
        .withData(businessCalendarService.createCalendar(request))
        .build();
  }

  @GetMapping
  @Operation(summary = "Get all active business calendars", description = "Retrieves all active business calendars")
  public ResponseEntity<ApiResponse<List<BusinessCalendarResponse>>> getAllActiveCalendars() {
    return ResponseBuilder.<List<BusinessCalendarResponse>>builder()
        .success()
        .withData(businessCalendarService.getAllActiveCalendars())
        .build();
  }

  @PostMapping("/{id}/holidays")
  @Operation(summary = "Add holiday to calendar", description = "Adds a holiday date to a specific business calendar")
  public ResponseEntity<ApiResponse<HolidayResponse>> addHoliday(
      @PathVariable Long id, @Valid @RequestBody CreateHolidayRequest request) {
    return ResponseBuilder.<HolidayResponse>builder()
        .created("Holiday added successfully")
        .withData(businessCalendarService.addHoliday(id, request))
        .build();
  }

  @GetMapping("/{id}/holidays")
  @Operation(summary = "Get calendar holidays", description = "Retrieves all holidays configured for a business calendar")
  public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidays(@PathVariable Long id) {
    return ResponseBuilder.<List<HolidayResponse>>builder()
        .success()
        .withData(businessCalendarService.getHolidays(id))
        .build();
  }
}
