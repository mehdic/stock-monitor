package com.stockmonitor.repository;

import com.stockmonitor.model.FactorModelVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactorModelVersionRepository extends JpaRepository<FactorModelVersion, UUID> {

  Optional<FactorModelVersion> findByIsActiveTrue();

  Optional<FactorModelVersion> findByVersionNumber(String versionNumber);
}
