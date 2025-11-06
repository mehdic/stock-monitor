package com.stockmonitor.repository;

import com.stockmonitor.model.Recommendation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

  List<Recommendation> findByRunId(UUID runId);

  List<Recommendation> findByRunIdOrderByRankAsc(UUID runId);

  Optional<Recommendation> findByRunIdAndSymbol(UUID runId, String symbol);

  List<Recommendation> findByRunIdAndSector(UUID runId, String sector);

  void deleteByRunId(UUID runId);
}
