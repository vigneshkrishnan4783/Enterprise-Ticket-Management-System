package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Site;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

  List<Site> findByOrganizationId(Long organizationId);

  Optional<Site> findByIdAndOrganizationId(Long id, Long organizationId);
}
