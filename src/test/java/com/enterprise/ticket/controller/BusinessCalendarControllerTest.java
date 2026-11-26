package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.enterprise.ticket.request.CreateBusinessCalendarRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.BusinessCalendarResponse;
import com.enterprise.ticket.response.HolidayResponse;
import com.enterprise.ticket.service.BusinessCalendarService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BusinessCalendarControllerTest {

  @Test
  @DisplayName("getAllActiveCalendars should return 200 OK and list of calendars")
  void testGetAllActiveCalendars() {
    BusinessCalendarService dummyService =
        new BusinessCalendarService(null, null) {
          @Override
          public List<BusinessCalendarResponse> getAllActiveCalendars() {
            return List.of(
                BusinessCalendarResponse.builder()
                    .id(1L)
                    .name("Standard")
                    .workStartTime(LocalTime.of(9, 0))
                    .workEndTime(LocalTime.of(18, 0))
                    .isDefault(true)
                    .build());
          }
        };

    BusinessCalendarController controller = new BusinessCalendarController(dummyService);
    ResponseEntity<ApiResponse<List<BusinessCalendarResponse>>> response =
        controller.getAllActiveCalendars();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals("Standard", response.getBody().getData().getFirst().getName());
  }

  @Test
  @DisplayName("createCalendar should return 201 Created and newly created calendar")
  void testCreateCalendar() {
    BusinessCalendarService dummyService =
        new BusinessCalendarService(null, null) {
          @Override
          public BusinessCalendarResponse createCalendar(CreateBusinessCalendarRequest req) {
            return BusinessCalendarResponse.builder().id(2L).name(req.getName()).build();
          }
        };

    BusinessCalendarController controller = new BusinessCalendarController(dummyService);
    CreateBusinessCalendarRequest request =
        CreateBusinessCalendarRequest.builder().name("Custom Calendar").build();

    ResponseEntity<ApiResponse<BusinessCalendarResponse>> response =
        controller.createCalendar(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(2L, response.getBody().getData().getId());
    assertEquals("Custom Calendar", response.getBody().getData().getName());
  }

  @Test
  @DisplayName("getHolidays should return 200 OK and list of holidays")
  void testGetHolidays() {
    BusinessCalendarService dummyService =
        new BusinessCalendarService(null, null) {
          @Override
          public List<HolidayResponse> getHolidays(Long calendarId) {
            return List.of(
                HolidayResponse.builder()
                    .id(10L)
                    .businessCalendarId(calendarId)
                    .holidayDate(LocalDate.of(2026, 1, 1))
                    .name("New Year")
                    .build());
          }
        };

    BusinessCalendarController controller = new BusinessCalendarController(dummyService);
    ResponseEntity<ApiResponse<List<HolidayResponse>>> response = controller.getHolidays(1L);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals("New Year", response.getBody().getData().getFirst().getName());
  }
}
