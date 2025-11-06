package com.stockmonitor.repository;

import com.stockmonitor.model.UniverseConstituent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniverseConstituentRepository extends JpaRepository<UniverseConstituent, UUID> {

  List<UniverseConstituent> findByUniverseIdAndIsActiveTrue(UUID universeId);

  Optional<UniverseConstituent> findByUniverseIdAndSymbol(UUID universeId, String symbol);

  List<UniverseConstituent> findBySymbol(String symbol);
}
