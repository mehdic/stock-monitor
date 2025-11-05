package com.stockmonitor.repository;

import com.stockmonitor.model.FactorScore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactorScoreRepository extends JpaRepository<FactorScore, UUID> {

  List<FactorScore> findBySymbolAndCalculationDate(String symbol, LocalDate calculationDate);

  Optional<FactorScore> findBySymbolAndFactorTypeAndCalculationDate(
      String symbol, String factorType, LocalDate calculationDate);

  List<FactorScore> findByCalculationDateAndFactorType(
      LocalDate calculationDate, String factorType);

  List<FactorScore> findByCalculationDate(LocalDate calculationDate);
}
