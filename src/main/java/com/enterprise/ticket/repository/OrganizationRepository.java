package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByCode(String code);

  boolean existsByCode(String code);
}
