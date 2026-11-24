package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Urgency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrgencyRepository extends JpaRepository<Urgency, Long> {
  List<Urgency> findByIsActiveTrueOrderByLevelWeightDesc();
  Optional<Urgency> findByCode(String code);
  boolean existsByCode(String code);
  boolean existsByName(String name);
}
