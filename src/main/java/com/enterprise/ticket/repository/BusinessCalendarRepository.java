package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.BusinessCalendar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessCalendarRepository extends JpaRepository<BusinessCalendar, Long> {

  Optional<BusinessCalendar> findByName(String name);

  Optional<BusinessCalendar> findByIsDefaultTrueAndIsActiveTrue();

  List<BusinessCalendar> findAllByIsActiveTrue();
}
