package com.stockmonitor.service;

import com.stockmonitor.model.FactorScore;
import com.stockmonitor.model.UniverseConstituent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating factor scores across multiple universe constituents.
 *
 * <p>Calculates factor scores (Value, Momentum, Quality, Revisions) for a list of constituents.
 * This is used by the recommendation engine for bulk factor calculations.
 */
@Service
@Slf4j
public class FactorCalculationService {

  private final Random random = new Random();

  /**
   * Calculate factor scores for multiple constituents.
   *
   * @param constituents List of universe constituents
   * @param factorType Factor type (e.g., "VALUE", "MOMENTUM", "QUALITY", "REVISIONS")
   * @param calculationDate Date of calculation
   * @return Map of symbol to FactorScore
   */
  public Map<String, FactorScore> calculateFactorScores(
      List<UniverseConstituent> constituents, String factorType, LocalDate calculationDate) {

    log.debug(
        "Calculating {} factor scores for {} constituents on {}",
        factorType,
        constituents.size(),
        calculationDate);

    Map<String, FactorScore> scores = new HashMap<>();

    for (UniverseConstituent constituent : constituents) {
      // Stub implementation - generate mock scores
      // In production, this would fetch real fundamental data and calculate actual factors
      BigDecimal rawScore = generateMockScore(factorType);
      BigDecimal normalizedScore = normalizeScore(rawScore);

      FactorScore factorScore =
          FactorScore.builder()
              .symbol(constituent.getSymbol())
              .factorType(factorType)
              .calculationDate(calculationDate)
              .rawScore(rawScore)
              .sectorNormalizedScore(normalizedScore)
              .percentileRankSector(BigDecimal.valueOf(50.0))
              .percentileRankUniverse(BigDecimal.valueOf(50.0))
              .sector(constituent.getSector())
              .build();

      scores.put(constituent.getSymbol(), factorScore);
    }

    return scores;
  }

  /**
   * Generate mock factor score for testing.
   */
  private BigDecimal generateMockScore(String factorType) {
    // Generate random score between -2.0 and +2.0 (simulating z-scores)
    double score = (random.nextDouble() * 4.0) - 2.0;
    return BigDecimal.valueOf(score);
  }

  /**
   * Normalize raw score to z-score.
   */
  private BigDecimal normalizeScore(BigDecimal rawScore) {
    // Stub - in production would calculate actual z-score based on universe distribution
    return rawScore;
  }
}
