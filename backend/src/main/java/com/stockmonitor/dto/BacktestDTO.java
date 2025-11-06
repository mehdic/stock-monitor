package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for backtest results (T175, FR-051 to FR-053).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestDTO {

  private UUID backtestId;
  private UUID portfolioId;
  private String status; // RUNNING, COMPLETED, FAILED
  private LocalDate startDate;
  private LocalDate endDate;

  /** CAGR (Compound Annual Growth Rate) as percentage. */
  private BigDecimal cagr;

  /** Sharpe ratio (risk-adjusted return). */
  private BigDecimal sharpeRatio;

  /** Maximum drawdown as percentage. */
  private BigDecimal maxDrawdown;

  /** Equity curve: {date, portfolioValue} time series. */
  private List<EquityPoint> equityCurve;

  /** Average turnover per rebalance. */
  private BigDecimal averageTurnover;

  /** Total number of trades executed. */
  private Integer tradeCount;

  /** Benchmark CAGR (S&P 500 equal weight). */
  private BigDecimal benchmarkCAGR;

  /** Did strategy beat equal weight benchmark after costs? */
  private Boolean beatEqualWeight;

  /** Total transaction costs incurred. */
  private BigDecimal totalTransactionCosts;

  /** Estimated completion time for running backtests. */
  private LocalDateTime estimatedCompletion;

  /** Error message if status = FAILED. */
  private String errorMessage;

  /**
   * Equity curve point.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EquityPoint {
    private LocalDate date;
    private BigDecimal portfolioValue;
    private BigDecimal benchmarkValue;
  }
}
