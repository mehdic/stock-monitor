package com.stockmonitor.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sensitivity analysis preview (T180, FR-054, FR-055).
 *
 * <p>Shows impact of constraint changes on key metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitivityPreviewDTO {

  /** Constraint that was modified. */
  private String constraintName;

  /** Original value. */
  private BigDecimal originalValue;

  /** New value being tested. */
  private BigDecimal newValue;

  /** Expected change in number of holdings. */
  private Integer expectedHoldingsDelta;

  /** Expected change in turnover (percentage points). */
  private BigDecimal expectedTurnoverDelta;

  /** Expected change in sector concentration. */
  private BigDecimal expectedSectorConcentrationDelta;

  /** Expected change in portfolio return (estimated). */
  private BigDecimal expectedReturnDelta;

  /** Expected change in portfolio risk (volatility). */
  private BigDecimal expectedRiskDelta;

  /** Sensitivity score: How much does this constraint affect portfolio (1-10). */
  private Integer sensitivityScore;

  /** Human-readable summary of impact. */
  private String impactSummary;

  /** Recommendation: Should user apply this change? */
  private String recommendation;
}
