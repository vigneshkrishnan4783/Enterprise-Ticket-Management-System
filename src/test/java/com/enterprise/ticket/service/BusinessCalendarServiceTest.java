package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.BusinessCalendar;
import com.enterprise.ticket.model.Holiday;
import com.enterprise.ticket.repository.BusinessCalendarRepository;
import com.enterprise.ticket.repository.HolidayRepository;
import com.enterprise.ticket.request.CreateBusinessCalendarRequest;
import com.enterprise.ticket.request.CreateHolidayRequest;
import com.enterprise.ticket.response.BusinessCalendarResponse;
import com.enterprise.ticket.response.HolidayResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessCalendarServiceTest {

  @Test
  @DisplayName("createCalendar should validate null payload")
  void testCreateCalendarNullPayload() {
    BusinessCalendarService service = new BusinessCalendarService(null, null);
    RestException ex = assertThrows(RestException.class, () -> service.createCalendar(null));
    assertEquals("Create business calendar payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("createCalendar should validate blank name")
  void testCreateCalendarBlankName() {
    BusinessCalendarService service = new BusinessCalendarService(null, null);
    CreateBusinessCalendarRequest req = CreateBusinessCalendarRequest.builder().name("   ").build();
    RestException ex = assertThrows(RestException.class, () -> service.createCalendar(req));
    assertEquals("Calendar name is required", ex.getMessage());
  }

  @Test
  @DisplayName("createCalendar should succeed and return response")
  void testCreateCalendarSuccess() {
    BusinessCalendarRepository dummyRepo =
        new DummyBusinessCalendarRepository() {
          @Override
          public Optional<BusinessCalendar> findByName(String name) {
            return Optional.empty();
          }

          @Override
          public <S extends BusinessCalendar> S save(S entity) {
            entity.setId(10L);
            return entity;
          }
        };

    BusinessCalendarService service = new BusinessCalendarService(dummyRepo, null);
    CreateBusinessCalendarRequest req =
        CreateBusinessCalendarRequest.builder()
            .name("Standard Hours")
            .timeZone("UTC")
            .workStartTime(LocalTime.of(9, 0))
            .workEndTime(LocalTime.of(18, 0))
            .workDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
            .isDefault(true)
            .build();

    BusinessCalendarResponse response = service.createCalendar(req);
    assertNotNull(response);
    assertEquals("Standard Hours", response.getName());
    assertEquals(10L, response.getId());
  }

  @Test
  @DisplayName("addHoliday should validate null payload")
  void testAddHolidayNullPayload() {
    BusinessCalendarService service = new BusinessCalendarService(null, null);
    RestException ex = assertThrows(RestException.class, () -> service.addHoliday(1L, null));
    assertEquals("Create holiday payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("addHoliday should validate null date")
  void testAddHolidayNullDate() {
    BusinessCalendarService service = new BusinessCalendarService(null, null);
    CreateHolidayRequest req = CreateHolidayRequest.builder().name("New Year").build();
    RestException ex = assertThrows(RestException.class, () -> service.addHoliday(1L, req));
    assertEquals("Holiday date is required", ex.getMessage());
  }

  @Test
  @DisplayName("addHoliday should succeed")
  void testAddHolidaySuccess() {
    BusinessCalendar calendar = BusinessCalendar.builder().id(1L).name("Default").build();

    BusinessCalendarRepository dummyCalendarRepo =
        new DummyBusinessCalendarRepository() {
          @Override
          public Optional<BusinessCalendar> findById(Long id) {
            return Optional.of(calendar);
          }
        };

    HolidayRepository dummyHolidayRepo =
        new DummyHolidayRepository() {
          @Override
          public boolean existsByBusinessCalendarIdAndHolidayDate(Long calendarId, LocalDate date) {
            return false;
          }

          @Override
          public <S extends Holiday> S save(S entity) {
            entity.setId(50L);
            return entity;
          }
        };

    BusinessCalendarService service =
        new BusinessCalendarService(dummyCalendarRepo, dummyHolidayRepo);

    CreateHolidayRequest req =
        CreateHolidayRequest.builder().holidayDate(LocalDate.of(2026, 12, 25)).name("Christmas").build();

    HolidayResponse response = service.addHoliday(1L, req);
    assertNotNull(response);
    assertEquals(50L, response.getId());
    assertEquals("Christmas", response.getName());
  }

  private static class DummyBusinessCalendarRepository implements BusinessCalendarRepository {
    @Override public Optional<BusinessCalendar> findByName(String name) { return Optional.empty(); }
    @Override public Optional<BusinessCalendar> findByIsDefaultTrueAndIsActiveTrue() { return Optional.empty(); }
    @Override public List<BusinessCalendar> findAllByIsActiveTrue() { return List.of(); }
    @Override public void flush() {}
    @Override public <S extends BusinessCalendar> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends BusinessCalendar> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<BusinessCalendar> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public BusinessCalendar getOne(Long aLong) { return null; }
    @Override public BusinessCalendar getById(Long aLong) { return null; }
    @Override public BusinessCalendar getReferenceById(Long aLong) { return null; }
    @Override public <S extends BusinessCalendar> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
    @Override public <S extends BusinessCalendar> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
    @Override public <S extends BusinessCalendar> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public <S extends BusinessCalendar> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    @Override public <S extends BusinessCalendar> long count(org.springframework.data.domain.Example<S> example) { return 0; }
    @Override public <S extends BusinessCalendar> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
    @Override public <S extends BusinessCalendar, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends BusinessCalendar> S save(S entity) { return entity; }
    @Override public <S extends BusinessCalendar> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public Optional<BusinessCalendar> findById(Long aLong) { return Optional.empty(); }
    @Override public boolean existsById(Long aLong) { return false; }
    @Override public List<BusinessCalendar> findAll() { return List.of(); }
    @Override public List<BusinessCalendar> findAllById(Iterable<Long> longs) { return List.of(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long aLong) {}
    @Override public void delete(BusinessCalendar entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends BusinessCalendar> entities) {}
    @Override public void deleteAll() {}
    @Override public List<BusinessCalendar> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public org.springframework.data.domain.Page<BusinessCalendar> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
  }

  private static class DummyHolidayRepository implements HolidayRepository {
    @Override public List<Holiday> findByBusinessCalendarIdOrderByHolidayDateAsc(Long businessCalendarId) { return List.of(); }
    @Override public boolean existsByBusinessCalendarIdAndHolidayDate(Long businessCalendarId, LocalDate holidayDate) { return false; }
    @Override public Optional<Holiday> findByBusinessCalendarIdAndHolidayDate(Long businessCalendarId, LocalDate holidayDate) { return Optional.empty(); }
    @Override public void flush() {}
    @Override public <S extends Holiday> S saveAndFlush(S entity) { return entity; }
    @Override public <S extends Holiday> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<Holiday> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public Holiday getOne(Long aLong) { return null; }
    @Override public Holiday getById(Long aLong) { return null; }
    @Override public Holiday getReferenceById(Long aLong) { return null; }
    @Override public <S extends Holiday> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
    @Override public <S extends Holiday> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
    @Override public <S extends Holiday> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public <S extends Holiday> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    @Override public <S extends Holiday> long count(org.springframework.data.domain.Example<S> example) { return 0; }
    @Override public <S extends Holiday> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
    @Override public <S extends Holiday, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Holiday> S save(S entity) { return entity; }
    @Override public <S extends Holiday> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public Optional<Holiday> findById(Long aLong) { return Optional.empty(); }
    @Override public boolean existsById(Long aLong) { return false; }
    @Override public List<Holiday> findAll() { return List.of(); }
    @Override public List<Holiday> findAllById(Iterable<Long> longs) { return List.of(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long aLong) {}
    @Override public void delete(Holiday entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends Holiday> entities) {}
    @Override public void deleteAll() {}
    @Override public List<Holiday> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
    @Override public org.springframework.data.domain.Page<Holiday> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
  }
}
