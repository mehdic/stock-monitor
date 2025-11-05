package com.stockmonitor.engine;

import com.stockmonitor.dto.FactorScoreDTO;
import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core recommendation engine (FR-001, FR-003, FR-005).
 *
 * <p>Orchestrates the recommendation generation process: 1. Fetch universe constituents 2.
 * Calculate factor scores 3. Apply constraints 4. Rank and weight positions 5. Generate trade
 * recommendations
 */
@Service("coreRecommendationEngine")
@RequiredArgsConstructor
@Slf4j
public class RecommendationEngine {

  private final UniverseConstituentRepository universeConstituentRepository;
  private final FactorCalculationService factorCalculationService;
  private final ConstraintEvaluationService constraintEvaluationService;
  private final RecommendationRepository recommendationRepository;
  private final HoldingRepository holdingRepository;
  private final PortfolioRepository portfolioRepository;

  /**
   * Generate recommendations for a given universe and constraint set.
   *
   * @param run Recommendation run
   * @param universe Universe
   * @param constraints Constraint set
   * @return List of generated recommendations
   */
  @Transactional
  public List<Recommendation> generateRecommendations(
      RecommendationRun run, Universe universe, ConstraintSet constraints) {
    log.info("Generating recommendations for run {}", run.getId());

    // 1. Fetch universe constituents
    List<UniverseConstituent> constituents =
        universeConstituentRepository.findByUniverseIdAndIsActiveTrue(universe.getId());
    log.info("Fetched {} constituents from universe {}", constituents.size(), universe.getName());

    // 2. Get current holdings for portfolio (find by userId since run doesn't have portfolioId)
    Portfolio portfolio =
        portfolioRepository
            .findByUserId(run.getUserId())
            .orElseThrow(() -> new IllegalStateException("No portfolio found for user"));
    List<Holding> currentHoldings = holdingRepository.findByPortfolioId(portfolio.getId());

    // 3. Calculate factor scores for each constituent
    List<Recommendation> recommendations = new ArrayList<>();
    int rank = 1;

    for (UniverseConstituent constituent : constituents) {
      // Skip if doesn't meet liquidity requirements
      if (!constraintEvaluationService.meetsLiquidityRequirement(constituent, constraints)) {
        log.debug("Skipping {} due to liquidity requirements", constituent.getSymbol());
        continue;
      }

      // Create recommendation stub
      Recommendation recommendation =
          Recommendation.builder()
              .id(UUID.randomUUID())
              .runId(run.getId())
              .symbol(constituent.getSymbol())
              .sector(constituent.getSector())
              .marketCapTier(constituent.getMarketCapTier())
              .liquidityTier(constituent.getLiquidityTier())
              .rank(rank++)
              .targetWeightPct(BigDecimal.valueOf(1.0)) // Placeholder
              .confidenceScore(75) // Placeholder
              .expectedAlphaBps(BigDecimal.valueOf(50)) // Placeholder
              .expectedCostBps(BigDecimal.valueOf(10)) // Placeholder - FIXED field name
              .edgeOverCostBps(BigDecimal.valueOf(40)) // Placeholder
              .changeIndicator("NEW") // Will be updated by ChangeDetectionService
              .explanation("Factor-based recommendation") // Placeholder
              .driver1Name("Value")
              .driver1Score(BigDecimal.ZERO)
              .driver2Name("Momentum")
              .driver2Score(BigDecimal.ZERO)
              .driver3Name("Quality")
              .driver3Score(BigDecimal.ZERO)
              .build();

      recommendations.add(recommendation);
    }

    // 4. Save recommendations
    recommendationRepository.saveAll(recommendations);

    log.info("Generated {} recommendations for run {}", recommendations.size(), run.getId());
    return recommendations;
  }

  /**
   * Recalculate recommendations with new constraints (for preview).
   *
   * @param existingRecommendations Existing recommendations with factor scores
   * @param newConstraints New constraint set
   * @param currentHoldings Current holdings
   * @return Updated recommendations
   */
  public List<Recommendation> recalculateWithConstraints(
      List<Recommendation> existingRecommendations,
      ConstraintSet newConstraints,
      List<Holding> currentHoldings) {
    log.info(
        "Recalculating {} recommendations with new constraints", existingRecommendations.size());

    // Apply new constraints and re-rank
    List<Recommendation> filteredRecommendations =
        existingRecommendations.stream()
            .filter(
                rec ->
                    constraintEvaluationService.isWithinPositionSizeLimit(
                        rec.getTargetWeightPct(), rec.getMarketCapTier(), newConstraints))
            .collect(Collectors.toList());

    // Re-rank after filtering
    int rank = 1;
    for (Recommendation rec : filteredRecommendations) {
      rec.setRank(rank++);
    }

    log.info("Filtered to {} recommendations after applying constraints", filteredRecommendations.size());
    return filteredRecommendations;
  }
}
