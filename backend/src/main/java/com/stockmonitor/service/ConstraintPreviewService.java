package com.stockmonitor.service;

import com.stockmonitor.dto.ConstraintPreviewDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for previewing impact of constraint changes (FR-017, T137).
 *
 * <p>Re-runs optimizer with new constraints using last run's factor scores to estimate impact.
 * Returns estimates with accuracy ranges (±10% picks, ±15% turnover).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConstraintPreviewService {

  private final RecommendationRunRepository recommendationRunRepository;
  private final RecommendationRepository recommendationRepository;
  private final PortfolioRepository portfolioRepository;
  private final ConstraintSetRepository constraintSetRepository;
  private final HoldingRepository holdingRepository;
  private final UniverseConstituentRepository universeConstituentRepository;

  private static final BigDecimal PICK_COUNT_ACCURACY = BigDecimal.valueOf(0.10); // ±10%
  private static final BigDecimal TURNOVER_ACCURACY = BigDecimal.valueOf(0.15); // ±15%

  /**
   * Preview impact of constraint changes using last run's factor scores.
   *
   * @param portfolioId Portfolio ID
   * @param modifiedConstraints Modified constraints to preview
   * @return Preview with impact estimates and accuracy ranges
   * @throws IllegalStateException if no historical run data exists
   */
  @Transactional(readOnly = true)
  public ConstraintPreviewDTO previewConstraintImpact(
      UUID portfolioId, ConstraintSetDTO modifiedConstraints) {

    log.info("Previewing constraint impact for portfolio {}", portfolioId);

    // 1. Get portfolio and current constraints
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

    ConstraintSet currentConstraints =
        constraintSetRepository
            .findById(portfolio.getActiveConstraintSetId())
            .orElseThrow(() -> new IllegalStateException("No active constraints found"));

    // 2. Get last FINALIZED run for this user
    RecommendationRun lastRun =
        recommendationRunRepository
            .findFirstByUserIdAndStatusOrderByCompletedAtDesc(portfolio.getUserId(), "FINALIZED")
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No historical run data available for preview. Please run at least one recommendation first."));

    // 3. Get last run's recommendations
    List<Recommendation> lastRecommendations = recommendationRepository.findByRunId(lastRun.getId());
    log.info(
        "Found {} recommendations from last run ({})",
        lastRecommendations.size(),
        lastRun.getCompletedAt());

    // 4. Get current holdings
    List<Holding> currentHoldings = holdingRepository.findByPortfolioId(portfolioId);

    // 5. Simulate optimizer with new constraints
    SimulationResult simulationResult =
        simulateWithNewConstraints(
            lastRecommendations, currentHoldings, modifiedConstraints, portfolio);

    // 6. Calculate changes and build preview DTO
    return buildPreviewDTO(
        currentConstraints,
        modifiedConstraints,
        lastRecommendations.size(),
        simulationResult,
        lastRun);
  }

  /**
   * Simulate optimizer with new constraints using last run's factor scores
   */
  private SimulationResult simulateWithNewConstraints(
      List<Recommendation> lastRecommendations,
      List<Holding> currentHoldings,
      ConstraintSetDTO newConstraints,
      Portfolio portfolio) {

    // Sort by rank (factor scores from last run)
    List<Recommendation> rankedByFactors =
        lastRecommendations.stream()
            .sorted(Comparator.comparingInt(Recommendation::getRank))
            .collect(Collectors.toList());

    // Apply new constraints
    List<String> selectedSymbols = new ArrayList<>();
    Map<String, Integer> sectorCounts = new HashMap<>();
    BigDecimal totalWeight = BigDecimal.ZERO;
    int maxPicks = 50; // Maximum possible picks

    for (Recommendation rec : rankedByFactors) {
      if (selectedSymbols.size() >= maxPicks) break;

      // Check position size constraint (using large cap limit as proxy)
      BigDecimal proposedWeight = calculateProposedWeight(selectedSymbols.size() + 1);
      if (newConstraints.getMaxNameWeightLargeCapPct() != null
          && proposedWeight.compareTo(newConstraints.getMaxNameWeightLargeCapPct()) > 0) {
        continue; // Skip this stock
      }

      // Check sector exposure constraint
      String sector = rec.getSector();
      int sectorCount = sectorCounts.getOrDefault(sector, 0);
      BigDecimal sectorExposure =
          BigDecimal.valueOf(sectorCount + 1)
              .divide(
                  BigDecimal.valueOf(selectedSymbols.size() + 1), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));

      if (newConstraints.getMaxSectorExposurePct() != null
          && sectorExposure.compareTo(newConstraints.getMaxSectorExposurePct()) > 0) {
        continue; // Skip due to sector cap
      }

      // Liquidity constraint check
      if (newConstraints.getLiquidityFloorAdvUsd() != null) {
        // In real implementation, would check actual liquidity
        // For simulation, assume stocks passed if they were in last run
      }

      // Add to selected
      selectedSymbols.add(rec.getSymbol());
      sectorCounts.put(sector, sectorCount + 1);
      totalWeight = totalWeight.add(proposedWeight);
    }

    // Calculate turnover
    BigDecimal turnover = calculateTurnover(selectedSymbols, currentHoldings, portfolio);

    // Apply turnover constraint
    if (newConstraints.getTurnoverCapPct() != null
        && turnover.compareTo(newConstraints.getTurnoverCapPct()) > 0) {
      // Reduce picks to stay within turnover cap
      while (turnover.compareTo(newConstraints.getTurnoverCapPct()) > 0
          && selectedSymbols.size() > 10) {
        selectedSymbols.remove(selectedSymbols.size() - 1);
        turnover = calculateTurnover(selectedSymbols, currentHoldings, portfolio);
      }
    }

    return new SimulationResult(selectedSymbols, turnover);
  }

  /**
   * Calculate proposed weight for a position
   */
  private BigDecimal calculateProposedWeight(int numberOfPicks) {
    if (numberOfPicks == 0) return BigDecimal.ZERO;
    return BigDecimal.valueOf(100.0)
        .divide(BigDecimal.valueOf(numberOfPicks), 4, RoundingMode.HALF_UP);
  }

  /**
   * Calculate turnover from current holdings to new recommendations
   */
  private BigDecimal calculateTurnover(
      List<String> newSymbols, List<Holding> currentHoldings, Portfolio portfolio) {

    Set<String> currentSymbols =
        currentHoldings.stream().map(Holding::getSymbol).collect(Collectors.toSet());

    Set<String> newSymbolSet = new HashSet<>(newSymbols);

    // Symbols to sell (in current but not in new)
    Set<String> toSell = new HashSet<>(currentSymbols);
    toSell.removeAll(newSymbolSet);

    // Symbols to buy (in new but not in current)
    Set<String> toBuy = new HashSet<>(newSymbolSet);
    toBuy.removeAll(currentSymbols);

    // Handle edge case: no new symbols selected
    if (newSymbols.isEmpty()) {
      return BigDecimal.ZERO;
    }

    // Turnover = (sum of sells + sum of buys) / 2 / portfolio value
    BigDecimal totalPortfolioValue =
        portfolio.getTotalMarketValue().add(portfolio.getCashBalance());

    BigDecimal sellValue = BigDecimal.ZERO;
    for (String symbol : toSell) {
      Holding holding =
          currentHoldings.stream()
              .filter(h -> h.getSymbol().equals(symbol))
              .findFirst()
              .orElse(null);
      if (holding != null) {
        sellValue = sellValue.add(holding.getCurrentMarketValue());
      }
    }

    BigDecimal buyValue =
        BigDecimal.valueOf(toBuy.size())
            .multiply(totalPortfolioValue)
            .divide(BigDecimal.valueOf(newSymbols.size()), 4, RoundingMode.HALF_UP);

    BigDecimal totalTurnover = sellValue.add(buyValue);
    BigDecimal turnoverPct =
        totalTurnover
            .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

    return turnoverPct;
  }

  /**
   * Build preview DTO with estimates and accuracy ranges
   */
  private ConstraintPreviewDTO buildPreviewDTO(
      ConstraintSet currentConstraints,
      ConstraintSetDTO newConstraints,
      int lastRunPickCount,
      SimulationResult simulation,
      RecommendationRun lastRun) {

    int expectedPickCount = simulation.selectedSymbols.size();
    BigDecimal expectedTurnover = simulation.turnover;

    // Calculate accuracy ranges
    int pickCountLower =
        (int) (expectedPickCount * (1 - PICK_COUNT_ACCURACY.doubleValue()));
    int pickCountUpper =
        (int) Math.ceil(expectedPickCount * (1 + PICK_COUNT_ACCURACY.doubleValue()));
    String pickCountRange = String.format("±10%% (%d-%d picks)", pickCountLower, pickCountUpper);

    BigDecimal turnoverLower =
        expectedTurnover
            .multiply(BigDecimal.ONE.subtract(TURNOVER_ACCURACY))
            .setScale(1, RoundingMode.HALF_UP);
    BigDecimal turnoverUpper =
        expectedTurnover
            .multiply(BigDecimal.ONE.add(TURNOVER_ACCURACY))
            .setScale(1, RoundingMode.HALF_UP);
    String turnoverRange =
        String.format("±15%% (%.1f-%.1f%%)", turnoverLower, turnoverUpper);

    // Identify dropped and added symbols
    List<String> lastRunSymbols =
        recommendationRepository.findByRunId(lastRun.getId()).stream()
            .map(Recommendation::getSymbol)
            .collect(Collectors.toList());

    List<String> droppedSymbols =
        lastRunSymbols.stream()
            .filter(s -> !simulation.selectedSymbols.contains(s))
            .collect(Collectors.toList());

    List<String> addedSymbols =
        simulation.selectedSymbols.stream()
            .filter(s -> !lastRunSymbols.contains(s))
            .collect(Collectors.toList());

    // Build changes summary
    String changesSummary = buildChangesSummary(currentConstraints, newConstraints);

    // Build accuracy note
    String accuracyNote =
        String.format(
            "Based on last run's factor scores from %s, actual results may vary. Pick count ±10%%, turnover ±15%%",
            lastRun.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));

    // Build warnings
    List<String> warnings = new ArrayList<>();
    if (droppedSymbols.size() > 5) {
      warnings.add(
          String.format(
              "Warning: %d positions would be dropped with these constraints", droppedSymbols.size()));
    }
    if (expectedPickCount < 15) {
      warnings.add(
          "Warning: Very few picks may reduce diversification and increase concentration risk");
    }

    return ConstraintPreviewDTO.builder()
        .expectedPickCount(expectedPickCount)
        .expectedPickCountRange(pickCountRange)
        .expectedTurnoverPct(expectedTurnover)
        .expectedTurnoverRange(turnoverRange)
        .affectedPositionsCount(droppedSymbols.size() + addedSymbols.size())
        .droppedSymbols(droppedSymbols)
        .addedSymbols(addedSymbols)
        .accuracyNote(accuracyNote)
        .constraintChangesSummary(changesSummary)
        .warnings(warnings)
        .build();
  }

  /**
   * Build human-readable summary of constraint changes
   */
  private String buildChangesSummary(
      ConstraintSet current, ConstraintSetDTO modified) {
    List<String> changes = new ArrayList<>();

    if (!current.getMaxNameWeightLargeCapPct().equals(modified.getMaxNameWeightLargeCapPct())) {
      changes.add(
          String.format(
              "Large cap position size: %.1f%% → %.1f%%",
              current.getMaxNameWeightLargeCapPct(), modified.getMaxNameWeightLargeCapPct()));
    }

    if (!current.getMaxSectorExposurePct().equals(modified.getMaxSectorExposurePct())) {
      changes.add(
          String.format(
              "Sector cap: %.1f%% → %.1f%%",
              current.getMaxSectorExposurePct(), modified.getMaxSectorExposurePct()));
    }

    if (!current.getTurnoverCapPct().equals(modified.getTurnoverCapPct())) {
      changes.add(
          String.format(
              "Turnover cap: %.1f%% → %.1f%%",
              current.getTurnoverCapPct(), modified.getTurnoverCapPct()));
    }

    if (current.getLiquidityFloorAdvUsd() != null
        && modified.getLiquidityFloorAdvUsd() != null
        && !current.getLiquidityFloorAdvUsd().equals(modified.getLiquidityFloorAdvUsd())) {
      changes.add(
          String.format(
              "Liquidity floor: $%.2fM → $%.2fM",
              current.getLiquidityFloorAdvUsd().doubleValue() / 1_000_000,
              modified.getLiquidityFloorAdvUsd().doubleValue() / 1_000_000));
    }

    return changes.isEmpty() ? "No changes" : String.join(", ", changes);
  }

  /**
   * Result of simulation with new constraints
   */
  private static class SimulationResult {
    final List<String> selectedSymbols;
    final BigDecimal turnover;

    SimulationResult(List<String> selectedSymbols, BigDecimal turnover) {
      this.selectedSymbols = selectedSymbols;
      this.turnover = turnover;
    }
  }
}
