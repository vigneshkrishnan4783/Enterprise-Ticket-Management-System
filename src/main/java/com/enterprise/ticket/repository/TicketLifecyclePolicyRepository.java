package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.TicketLifecyclePolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketLifecyclePolicyRepository extends JpaRepository<TicketLifecyclePolicy, Long> {

  Optional<TicketLifecyclePolicy> findByPolicyNameIgnoreCase(String policyName);

  Optional<TicketLifecyclePolicy> findFirstByOrganizationIdAndIsActiveTrue(Long organizationId);

  Optional<TicketLifecyclePolicy> findFirstByOrganizationIsNullAndIsActiveTrue();

  List<TicketLifecyclePolicy> findAllByOrderByIdAsc();
}
