package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for portfolio performance metrics (T156, FR-008, FR-014).
 *
 * <p>Includes: - Total P&L (realized + unrealized) - Benchmark comparison - Top
 * contributors/detractors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsDTO {

  /** Total profit/loss in dollars (realized + unrealized). */
  private BigDecimal totalPnL;

  /** Total P&L as percentage of initial portfolio value. */
  private BigDecimal totalPnLPct;

  /** Benchmark return for the same period (e.g., S&P 500). */
  private BigDecimal benchmarkReturn;

  /** Excess return (portfolio return - benchmark return). */
  private BigDecimal excessReturn;

  /** Top 5 contributors by P&L. */
  private List<PerformanceContributorDTO> topContributors;

  /** Top 5 detractors by P&L. */
  private List<PerformanceContributorDTO> topDetractors;

  /** Start date of performance period. */
  private LocalDate periodStart;

  /** End date of performance period. */
  private LocalDate periodEnd;

  /** Portfolio value at period start. */
  private BigDecimal startingValue;

  /** Portfolio value at period end. */
  private BigDecimal endingValue;

  /** Number of trades executed during period. */
  private Integer tradeCount;

  /** Total transaction costs incurred. */
  private BigDecimal transactionCosts;

  /**
   * DTO for individual position contribution to performance (FR-014).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PerformanceContributorDTO {

    /** Stock symbol. */
    private String symbol;

    /** Contribution to total P&L in dollars. */
    private BigDecimal pnl;

    /** P&L as percentage of position cost basis. */
    private BigDecimal pnlPct;

    /** Position weight in portfolio. */
    private BigDecimal weight;

    /** Sector for context. */
    private String sector;

    /** Number of shares held. */
    private BigDecimal shares;

    /** Average cost basis per share. */
    private BigDecimal costBasis;

    /** Current price per share. */
    private BigDecimal currentPrice;
  }
}
