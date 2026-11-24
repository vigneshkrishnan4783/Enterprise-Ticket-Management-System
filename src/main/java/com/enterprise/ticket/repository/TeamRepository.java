package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
  List<Team> findByIsActiveTrue();
  Optional<Team> findByCode(String code);
  boolean existsByCode(String code);
  boolean existsByName(String name);
}
