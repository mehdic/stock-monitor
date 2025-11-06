package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for factor scores (T152, FR-034 to FR-036).
 *
 * <p>Factor scores are sector-normalized z-scores for: - Value: Valuation metrics (P/E, P/B, FCF
 * yield) - Momentum: Price returns (3M, 6M, 12M) - Quality: Profitability (ROE, margins,
 * leverage) - Revisions: Analyst estimate changes
 *
 * <p>All scores are z-scores relative to sector peers (mean=0, std=1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorScoreDTO {

  /** Value factor z-score (sector-normalized). */
  private BigDecimal value;

  /** Momentum factor z-score (sector-normalized). */
  private BigDecimal momentum;

  /** Quality factor z-score (sector-normalized). */
  private BigDecimal quality;

  /** Revisions factor z-score (sector-normalized). */
  private BigDecimal revisions;

  /** Composite score (weighted average of all factors). */
  private BigDecimal composite;

  /** Stock symbol this score applies to. */
  private String symbol;

  /** Sector for normalization context. */
  private String sector;

  /** Timestamp when factors were calculated. */
  private LocalDateTime calculatedAt;

  /** Raw value score before sector normalization (optional, for debugging). */
  private BigDecimal rawValue;

  /** Raw momentum score before sector normalization (optional). */
  private BigDecimal rawMomentum;

  /** Raw quality score before sector normalization (optional). */
  private BigDecimal rawQuality;

  /** Raw revisions score before sector normalization (optional). */
  private BigDecimal rawRevisions;

  /** Value percentile rank within sector (0-100). */
  private Integer valuePercentile;

  /** Momentum percentile rank within sector (0-100). */
  private Integer momentumPercentile;

  /** Quality percentile rank within sector (0-100). */
  private Integer qualityPercentile;

  /** Revisions percentile rank within sector (0-100). */
  private Integer revisionsPercentile;

  /**
   * Create simplified FactorScoreDTO with normalized scores only.
   *
   * @param value Value z-score
   * @param momentum Momentum z-score
   * @param quality Quality z-score
   * @param revisions Revisions z-score
   * @return FactorScoreDTO with normalized scores
   */
  public static FactorScoreDTO of(
      BigDecimal value, BigDecimal momentum, BigDecimal quality, BigDecimal revisions) {
    return FactorScoreDTO.builder()
        .value(value)
        .momentum(momentum)
        .quality(quality)
        .revisions(revisions)
        .composite(calculateComposite(value, momentum, quality, revisions))
        .build();
  }

  /**
   * Calculate composite score as weighted average.
   *
   * <p>Default weights: 25% each factor.
   *
   * @param value Value z-score
   * @param momentum Momentum z-score
   * @param quality Quality z-score
   * @param revisions Revisions z-score
   * @return Composite z-score
   */
  private static BigDecimal calculateComposite(
      BigDecimal value, BigDecimal momentum, BigDecimal quality, BigDecimal revisions) {
    BigDecimal sum =
        value.add(momentum).add(quality).add(revisions);
    return sum.divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
  }
}
