package com.stockmonitor.model;

/**
 * Status of a backtest execution.
 *
 * <p>Status flow: PENDING → RUNNING → COMPLETED/FAILED
 */
public enum BacktestStatus {
  /** Backtest created, waiting to start execution. */
  PENDING,

  /** Backtest currently executing (async processing). */
  RUNNING,

  /** Backtest completed successfully with results. */
  COMPLETED,

  /** Backtest failed due to error. */
  FAILED
}
