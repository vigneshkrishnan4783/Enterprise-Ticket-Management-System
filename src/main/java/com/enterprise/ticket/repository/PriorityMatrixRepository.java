package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.PriorityMatrix;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriorityMatrixRepository extends JpaRepository<PriorityMatrix, Long> {
  List<PriorityMatrix> findByIsActiveTrue();
  Optional<PriorityMatrix> findByImpactIdAndUrgencyId(Long impactId, Long urgencyId);
  boolean existsByImpactIdAndUrgencyId(Long impactId, Long urgencyId);
}
