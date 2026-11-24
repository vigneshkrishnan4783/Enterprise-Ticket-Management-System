package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Asset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

  List<Asset> findByOrganizationId(Long organizationId);

  Optional<Asset> findByIdAndOrganizationId(Long id, Long organizationId);

  Optional<Asset> findByAssetTag(String assetTag);
}
