package com.stockmonitor.repository;

import com.stockmonitor.model.Holding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

  List<Holding> findByPortfolioId(UUID portfolioId);

  Optional<Holding> findByPortfolioIdAndSymbol(UUID portfolioId, String symbol);

  List<Holding> findByPortfolioIdAndSector(UUID portfolioId, String sector);

  List<Holding> findByPortfolioIdAndInUniverseTrue(UUID portfolioId);

  void deleteByPortfolioId(UUID portfolioId);
}
