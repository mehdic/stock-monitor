package com.stockmonitor.engine;

import com.stockmonitor.model.ConstraintSet;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.UniverseConstituent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for evaluating constraints against universe constituents (FR-016).
 *
 * <p>Validates: - Position size limits by market cap tier - Sector exposure limits - Turnover
 * caps - Liquidity requirements - Trading cost thresholds
 */
@Service
@Slf4j
public class ConstraintEvaluationService {

  /**
   * Result of constraint evaluation.
   */
  public static class ConstraintEvaluationResult {
    private final boolean passed;
    private final List<String> violations;
    private final List<String> warnings;

    public ConstraintEvaluationResult(boolean passed, List<String> violations, List<String> warnings) {
      this.passed = passed;
      this.violations = violations;
      this.warnings = warnings;
    }

    public boolean passed() { return passed; }
    public List<String> violations() { return violations; }
    public List<String> warnings() { return warnings; }

    /**
     * Get combined notes from violations and warnings.
     */
    public String notes() {
      List<String> allNotes = new ArrayList<>();
      if (!violations.isEmpty()) {
        allNotes.add("VIOLATIONS: " + String.join("; ", violations));
      }
      if (!warnings.isEmpty()) {
        allNotes.add("WARNINGS: " + String.join("; ", warnings));
      }
      return allNotes.isEmpty() ? "" : String.join(" | ", allNotes);
    }

    public static ConstraintEvaluationResult pass() {
      return new ConstraintEvaluationResult(true, new ArrayList<>(), new ArrayList<>());
    }

    public static ConstraintEvaluationResult fail(List<String> violations) {
      return new ConstraintEvaluationResult(false, violations, new ArrayList<>());
    }

    public static ConstraintEvaluationResult passWithWarnings(List<String> warnings) {
      return new ConstraintEvaluationResult(true, new ArrayList<>(), warnings);
    }
  }

  /**
   * Evaluate all constraints for a single position.
   *
   * @param symbol Stock symbol
   * @param targetWeight Target weight percentage
   * @param constituent Universe constituent
   * @param constraints Constraint set
   * @param currentHoldings Current portfolio holdings
   * @return Evaluation result
   */
  public ConstraintEvaluationResult evaluateConstraints(
      String symbol,
      BigDecimal targetWeight,
      UniverseConstituent constituent,
      ConstraintSet constraints,
      List<Holding> currentHoldings) {

    List<String> violations = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Check position size limit
    if (!isWithinPositionSizeLimit(targetWeight, constituent.getMarketCapTier(), constraints)) {
      BigDecimal maxWeight = getMaxWeightForTier(constituent.getMarketCapTier(), constraints);
      violations.add(
          String.format(
              "Position size exceeds %s cap limit of %.1f%%",
              constituent.getMarketCapTier(), maxWeight));
    }

    // Check liquidity requirements
    if (!meetsLiquidityRequirement(constituent, constraints)) {
      violations.add(
          String.format(
              "Stock %s does not meet liquidity requirements (Tier %d)",
              symbol, constituent.getLiquidityTier()));
    }

    // Check weight deadband for existing holdings
    Holding currentHolding = findHolding(symbol, currentHoldings);
    if (currentHolding != null) {
      // Calculate total portfolio value
      BigDecimal totalPortfolioValue = BigDecimal.ZERO;
      for (Holding holding : currentHoldings) {
        totalPortfolioValue = totalPortfolioValue.add(getHoldingMarketValue(holding));
      }

      // Calculate current weight as % of portfolio
      BigDecimal currentWeight = BigDecimal.ZERO;
      if (totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal marketValue = getHoldingMarketValue(currentHolding);
        currentWeight = marketValue.divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
      }

      if (!exceedsWeightDeadband(currentWeight, targetWeight, constraints)) {
        warnings.add(
            String.format(
                "Weight change for %s is within deadband threshold (small change)", symbol));
      }
    }

    if (violations.isEmpty()) {
      return warnings.isEmpty()
          ? ConstraintEvaluationResult.pass()
          : ConstraintEvaluationResult.passWithWarnings(warnings);
    } else {
      return ConstraintEvaluationResult.fail(violations);
    }
  }

  /**
   * Calculate portfolio turnover.
   *
   * @param currentHoldings Current holdings
   * @param targetSymbols Target symbols
   * @param targetWeights Target weights
   * @return Turnover percentage
   */
  public BigDecimal calculateTurnover(
      List<Holding> currentHoldings, List<String> targetSymbols, List<BigDecimal> targetWeights) {

    // Calculate total portfolio value from current holdings
    BigDecimal totalPortfolioValue = BigDecimal.ZERO;
    for (Holding holding : currentHoldings) {
      BigDecimal marketValue = getHoldingMarketValue(holding);
      totalPortfolioValue = totalPortfolioValue.add(marketValue);
    }

    // If no current holdings, all target weights are new positions
    if (totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
      // Sum of all target weights represents new positions
      BigDecimal totalNewWeight = targetWeights.stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      // Turnover = total new positions / 2 (one-sided turnover)
      return totalNewWeight.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
    }

    BigDecimal totalChange = BigDecimal.ZERO;

    // Calculate total absolute weight changes for existing and new positions
    for (int i = 0; i < targetSymbols.size(); i++) {
      String symbol = targetSymbols.get(i);
      BigDecimal targetWeight = targetWeights.get(i);

      Holding current = findHolding(symbol, currentHoldings);
      BigDecimal currentWeight = BigDecimal.ZERO;

      if (current != null) {
        // Calculate current weight as % of portfolio
        BigDecimal marketValue = getHoldingMarketValue(current);
        currentWeight = marketValue.divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
      }

      BigDecimal change = targetWeight.subtract(currentWeight).abs();
      totalChange = totalChange.add(change);
    }

    // Add weight from sold positions (in current but not in target)
    for (Holding holding : currentHoldings) {
      if (!targetSymbols.contains(holding.getSymbol())) {
        BigDecimal marketValue = getHoldingMarketValue(holding);
        BigDecimal currentWeight = marketValue.divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        totalChange = totalChange.add(currentWeight);
      }
    }

    // Turnover = total absolute changes / 2
    return totalChange.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
  }

  /**
   * Calculate market value of a holding.
   *
   * @param holding Holding
   * @return Market value
   */
  private BigDecimal getHoldingMarketValue(Holding holding) {
    if (holding.getCurrentPrice() != null && holding.getQuantity() != null) {
      return holding.getQuantity().multiply(holding.getCurrentPrice());
    }
    // Fallback: use quantity as market value if price not available
    return holding.getQuantity() != null ? holding.getQuantity() : BigDecimal.ZERO;
  }

  private Holding findHolding(String symbol, List<Holding> holdings) {
    return holdings.stream()
        .filter(h -> h.getSymbol().equals(symbol))
        .findFirst()
        .orElse(null);
  }

  private BigDecimal getMaxWeightForTier(String marketCapTier, ConstraintSet constraints) {
    return switch (marketCapTier) {
      case "LARGE_CAP", "Large" -> constraints.getMaxNameWeightLargeCapPct();
      case "MID_CAP", "Mid" -> constraints.getMaxNameWeightMidCapPct();
      case "SMALL_CAP", "Small" -> constraints.getMaxNameWeightSmallCapPct();
      default -> BigDecimal.ZERO;
    };
  }

  /**
   * Check if position size is within limits for a given market cap tier.
   *
   * @param weight Position weight (%)
   * @param marketCapTier Market cap tier (LARGE_CAP, MID_CAP, SMALL_CAP or Large, Mid, Small)
   * @param constraints Constraint set
   * @return true if within limits
   */
  public boolean isWithinPositionSizeLimit(
      BigDecimal weight, String marketCapTier, ConstraintSet constraints) {
    BigDecimal maxWeight =
        switch (marketCapTier) {
          case "LARGE_CAP", "Large" -> constraints.getMaxNameWeightLargeCapPct();
          case "MID_CAP", "Mid" -> constraints.getMaxNameWeightMidCapPct();
          case "SMALL_CAP", "Small" -> constraints.getMaxNameWeightSmallCapPct();
          default -> BigDecimal.ZERO;
        };

    return weight.compareTo(maxWeight) <= 0;
  }

  /**
   * Check if sector exposure is within limits.
   *
   * @param sectorWeight Total sector weight (%)
   * @param constraints Constraint set
   * @return true if within limits
   */
  public boolean isWithinSectorLimit(BigDecimal sectorWeight, ConstraintSet constraints) {
    return sectorWeight.compareTo(constraints.getMaxSectorExposurePct()) <= 0;
  }

  /**
   * Check if turnover is within cap.
   *
   * @param turnover Turnover percentage
   * @param constraints Constraint set
   * @return true if within cap
   */
  public boolean isWithinTurnoverCap(BigDecimal turnover, ConstraintSet constraints) {
    return turnover.compareTo(constraints.getTurnoverCapPct()) <= 0;
  }

  /**
   * Check if liquidity meets minimum threshold.
   *
   * @param constituent Universe constituent
   * @param constraints Constraint set
   * @return true if liquidity sufficient
   */
  public boolean meetsLiquidityRequirement(
      UniverseConstituent constituent, ConstraintSet constraints) {
    if (constituent.getLiquidityTier() == null || constraints.getLiquidityFloorAdvUsd() == null) {
      return true; // No constraint to check
    }

    // Liquidity tier 1 = highest liquidity (best)
    // Liquidity tier 5 = lowest liquidity (worst)
    // For now, accept tiers 1-4, reject tier 5 if there's a liquidity floor
    return constituent.getLiquidityTier() <= 4;
  }

  /**
   * Check if trading cost is acceptable.
   *
   * @param estimatedCostBps Estimated trading cost (basis points)
   * @param constraints Constraint set
   * @return true if cost acceptable
   */
  public boolean isTradingCostAcceptable(Integer estimatedCostBps, ConstraintSet constraints) {
    if (estimatedCostBps == null || constraints.getCostMarginRequiredBps() == null) {
      return true;
    }

    // Trading cost should not exceed margin required
    return estimatedCostBps <= constraints.getCostMarginRequiredBps();
  }

  /**
   * Check if weight change exceeds deadband threshold.
   *
   * @param currentWeight Current weight (%)
   * @param targetWeight Target weight (%)
   * @param constraints Constraint set
   * @return true if change exceeds deadband (trade should occur)
   */
  public boolean exceedsWeightDeadband(
      BigDecimal currentWeight, BigDecimal targetWeight, ConstraintSet constraints) {
    if (constraints.getWeightDeadbandBps() == null) {
      return true; // No deadband, always trade
    }

    // Convert bps to percent: bps / 100 = percent (e.g., 10 bps = 0.10%)
    BigDecimal deadbandPct =
        BigDecimal.valueOf(constraints.getWeightDeadbandBps()).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
    BigDecimal weightChange = targetWeight.subtract(currentWeight).abs();

    return weightChange.compareTo(deadbandPct) > 0;
  }
}
