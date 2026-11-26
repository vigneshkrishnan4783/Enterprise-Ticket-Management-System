package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

  List<Holiday> findByBusinessCalendarIdOrderByHolidayDateAsc(Long businessCalendarId);

  boolean existsByBusinessCalendarIdAndHolidayDate(Long businessCalendarId, LocalDate holidayDate);

  Optional<Holiday> findByBusinessCalendarIdAndHolidayDate(Long businessCalendarId, LocalDate holidayDate);
}
