package com.stockmonitor.service;

import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import com.stockmonitor.engine.ConstraintEvaluationService;
import com.stockmonitor.engine.ConstraintEvaluationService.ConstraintEvaluationResult;
import com.stockmonitor.service.ExplanationService.FactorDriver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core recommendation engine that generates portfolio recommendations.
 *
 * <p>Combines factor scores with constraint evaluation to produce ranked list of buy/sell
 * recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEngine {

  private final FactorCalculationService factorCalculationService;
  private final ConstraintEvaluationService constraintEvaluationService;
  private final ExplanationService explanationService;
  private final UniverseConstituentRepository universeConstituentRepository;
  private final HoldingRepository holdingRepository;
  private final RecommendationRepository recommendationRepository;
  private final com.stockmonitor.controller.RunStatusWebSocketController runStatusWebSocketController;

  /**
   * Generate recommendations for a portfolio.
   *
   * @param run RecommendationRun configuration
   * @param universe Universe to select from
   * @param constraints Active constraint set
   * @param portfolio User's portfolio
   * @return List of generated recommendations
   */
  @Transactional
  public List<Recommendation> generateRecommendations(
      RecommendationRun run, Universe universe, ConstraintSet constraints, Portfolio portfolio) {

    log.info(
        "Generating recommendations for run {} using universe {} and constraints {}",
        run.getId(),
        universe.getName(),
        constraints.getName());

    // Progress: Starting (0%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 0, "Starting recommendation generation");

    // 1. Get universe constituents
    List<UniverseConstituent> constituents =
        universeConstituentRepository.findByUniverseIdAndIsActiveTrue(universe.getId());
    log.info("Universe contains {} active constituents", constituents.size());

    // Progress: Universe loaded (10%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 10, "Loaded " + constituents.size() + " universe constituents");

    // 2. Calculate factor scores for all constituents
    LocalDate calculationDate = LocalDate.now();
    List<String> factorTypes = Arrays.asList("VALUE", "MOMENTUM", "QUALITY");

    Map<String, Map<String, FactorScore>> allFactorScores = new HashMap<>();

    for (String factorType : factorTypes) {
      Map<String, FactorScore> scores =
          factorCalculationService.calculateFactorScores(constituents, factorType, calculationDate);
      for (Map.Entry<String, FactorScore> entry : scores.entrySet()) {
        allFactorScores.putIfAbsent(entry.getKey(), new HashMap<>());
        allFactorScores.get(entry.getKey()).put(factorType, entry.getValue());
      }
    }

    // Progress: Factor scores calculated (40%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 40, "Calculated factor scores for all stocks");

    // 3. Calculate composite scores (equal-weighted factors)
    Map<String, BigDecimal> compositeScores = new HashMap<>();
    for (Map.Entry<String, Map<String, FactorScore>> entry : allFactorScores.entrySet()) {
      BigDecimal composite =
          entry.getValue().values().stream()
              .map(FactorScore::getSectorNormalizedScore)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .divide(
                  BigDecimal.valueOf(entry.getValue().size()), 6, RoundingMode.HALF_UP);
      compositeScores.put(entry.getKey(), composite);
    }

    // Progress: Composite scores calculated (50%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 50, "Calculated composite scores");

    // 4. Rank stocks by composite score
    List<Map.Entry<String, BigDecimal>> rankedStocks =
        compositeScores.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(Collectors.toList());

    // Progress: Stocks ranked (60%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 60, "Ranked " + rankedStocks.size() + " stocks by composite score");

    // 5. Get current holdings
    List<Holding> currentHoldings = holdingRepository.findByPortfolioId(portfolio.getId());
    BigDecimal totalPortfolioValue = portfolio.getTotalMarketValue().add(portfolio.getCashBalance());

    // 6. Generate recommendations for top stocks
    List<Recommendation> recommendations = new ArrayList<>();
    int targetHoldings = 30; // Target 30 holdings (simplified)
    BigDecimal targetWeightPerPosition = BigDecimal.valueOf(100.0 / targetHoldings);

    // Progress: Starting recommendation generation (70%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 70, "Generating recommendations for top " + targetHoldings + " stocks");

    for (int i = 0; i < Math.min(targetHoldings, rankedStocks.size()); i++) {
      Map.Entry<String, BigDecimal> rankedStock = rankedStocks.get(i);
      String symbol = rankedStock.getKey();
      int rank = i + 1;

      // Get constituent data
      UniverseConstituent constituent =
          constituents.stream()
              .filter(c -> c.getSymbol().equals(symbol))
              .findFirst()
              .orElse(null);

      if (constituent == null) {
        log.warn("Constituent data not found for symbol: {}", symbol);
        continue;
      }

      // Get factor scores
      Map<String, FactorScore> symbolFactorScores = allFactorScores.get(symbol);

      // Evaluate constraints
      ConstraintEvaluationResult constraintResult =
          constraintEvaluationService.evaluateConstraints(
              symbol, targetWeightPerPosition, constituent, constraints, currentHoldings);

      // Calculate current weight
      BigDecimal currentWeight =
          currentHoldings.stream()
              .filter(h -> h.getSymbol().equals(symbol))
              .findFirst()
              .map(
                  h ->
                      h.getCurrentMarketValue()
                          .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100)))
              .orElse(BigDecimal.ZERO);

      // Weight change
      BigDecimal weightChange = targetWeightPerPosition.subtract(currentWeight);

      // Identify top drivers
      List<FactorDriver> topDrivers =
          explanationService.identifyTopDrivers(symbolFactorScores);

      // Generate explanation
      String explanation =
          explanationService.generateExplanation(
              symbol, rank, symbolFactorScores, topDrivers, constraintResult.notes());

      // Calculate metrics
      int confidenceScore = calculateConfidenceScore(compositeScores.get(symbol), rank);
      BigDecimal expectedCostBps = estimateCost(constituent.getLiquidityTier(), weightChange);
      BigDecimal expectedAlphaBps =
          compositeScores.get(symbol).multiply(BigDecimal.valueOf(100));
      BigDecimal edgeOverCostBps = expectedAlphaBps.subtract(expectedCostBps);

      // Create recommendation
      Recommendation recommendation =
          Recommendation.builder()
              .runId(run.getId())
              .symbol(symbol)
              .rank(rank)
              .targetWeightPct(targetWeightPerPosition)
              .currentWeightPct(currentWeight)
              .weightChangePct(weightChange)
              .confidenceScore(confidenceScore)
              .expectedCostBps(expectedCostBps)
              .expectedAlphaBps(expectedAlphaBps)
              .edgeOverCostBps(edgeOverCostBps)
              .driver1Name(topDrivers.size() > 0 ? topDrivers.get(0).factorName() : "N/A")
              .driver1Score(
                  topDrivers.size() > 0 ? topDrivers.get(0).score() : BigDecimal.ZERO)
              .driver2Name(topDrivers.size() > 1 ? topDrivers.get(1).factorName() : "N/A")
              .driver2Score(
                  topDrivers.size() > 1 ? topDrivers.get(1).score() : BigDecimal.ZERO)
              .driver3Name(topDrivers.size() > 2 ? topDrivers.get(2).factorName() : "N/A")
              .driver3Score(
                  topDrivers.size() > 2 ? topDrivers.get(2).score() : BigDecimal.ZERO)
              .explanation(explanation)
              .constraintNotes(constraintResult.notes())
              .riskContributionPct(BigDecimal.valueOf(100.0 / targetHoldings))
              .changeIndicator(currentWeight.compareTo(BigDecimal.ZERO) == 0 ? "NEW" : "MODIFY")
              .sector(constituent.getSector())
              .marketCapTier(constituent.getMarketCapTier())
              .liquidityTier(constituent.getLiquidityTier())
              .currentPrice(BigDecimal.valueOf(100.0)) // Placeholder - would fetch real price
              .build();

      recommendations.add(recommendationRepository.save(recommendation));
    }

    // Progress: Recommendations completed (100%)
    runStatusWebSocketController.sendStatusUpdate(
        run.getId(), "RUNNING", 100, "Generated " + recommendations.size() + " recommendations successfully");

    log.info("Generated {} recommendations for run {}", recommendations.size(), run.getId());
    return recommendations;
  }

  private int calculateConfidenceScore(BigDecimal compositeScore, int rank) {
    // Confidence based on composite score strength and rank
    double baseConfidence = Math.min(Math.max(compositeScore.doubleValue() * 30 + 50, 0), 100);

    // Reduce confidence for lower ranks
    double rankPenalty = (rank - 1) * 0.5;
    int confidence = (int) Math.max(baseConfidence - rankPenalty, 10);

    return Math.min(confidence, 100);
  }

  private BigDecimal estimateCost(Integer liquidityTier, BigDecimal weightChange) {
    // Cost estimation based on liquidity tier and trade size
    int baseSpreadBps =
        switch (liquidityTier) {
          case 1 -> 5;
          case 2 -> 10;
          case 3 -> 20;
          case 4 -> 40;
          case 5 -> 80;
          default -> 30;
        };

    // Market impact scales with trade size
    double impactMultiplier = 1.0 + (weightChange.abs().doubleValue() / 100.0);

    return BigDecimal.valueOf(baseSpreadBps * impactMultiplier);
  }
}
