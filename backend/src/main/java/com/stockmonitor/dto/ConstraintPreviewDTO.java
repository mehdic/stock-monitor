package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for constraint preview impact estimates (FR-017, T136).
 *
 * <p>Provides estimates of how constraint changes would affect next recommendation run based on
 * last run's factor scores. Includes accuracy ranges per FR-017 (±10% picks, ±15% turnover).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintPreviewDTO {

  /**
   * Expected number of recommendations (picks) with new constraints
   *
   * <p>Example: 28 (down from 30 with current constraints)
   */
  private Integer expectedPickCount;

  /**
   * Accuracy range for pick count estimate
   *
   * <p>Example: "±10% (25-31 picks)"
   */
  private String expectedPickCountRange;

  /**
   * Expected portfolio turnover percentage with new constraints
   *
   * <p>Example: 18.5% (down from 23% with current constraints)
   */
  private BigDecimal expectedTurnoverPct;

  /**
   * Accuracy range for turnover estimate
   *
   * <p>Example: "±15% (15.7-21.3%)"
   */
  private String expectedTurnoverRange;

  /**
   * Number of positions affected by constraint change
   *
   * <p>Count of positions that would be dropped or added. Equal to sum of droppedSymbols.size() +
   * addedSymbols.size()
   */
  private Integer affectedPositionsCount;

  /**
   * List of symbols that would be dropped due to new constraints
   *
   * <p>Example: ["AAPL", "MSFT"] - dropped because new sector cap is tighter
   */
  private List<String> droppedSymbols;

  /**
   * List of symbols that would be added with new constraints
   *
   * <p>Example: ["NVDA", "GOOGL"] - newly eligible under relaxed constraints
   */
  private List<String> addedSymbols;

  /**
   * Note about accuracy of estimates
   *
   * <p>Standard note: "Based on last run's factor scores from {date}, actual results may vary.
   * Pick count ±10%, turnover ±15%"
   */
  private String accuracyNote;

  /**
   * Constraint changes summary for display
   *
   * <p>Example: "Turnover cap: 25% → 20%, Market cap floor: $1B → $2B"
   */
  private String constraintChangesSummary;

  /**
   * Additional warnings or notes
   *
   * <p>Example: "Warning: Tighter sector cap may significantly reduce picks in Technology sector"
   */
  private List<String> warnings;
}
