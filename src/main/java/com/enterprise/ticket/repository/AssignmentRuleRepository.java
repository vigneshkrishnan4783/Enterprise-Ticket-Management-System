package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.AssignmentRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {

  List<AssignmentRule> findByIsActiveTrueOrderByPriorityOrderAsc();

  Optional<AssignmentRule> findByName(String name);

  boolean existsByName(String name);
}
