package com.stockmonitor.service;

import com.stockmonitor.dto.BacktestConstraintsDTO;
import com.stockmonitor.dto.BacktestDTO;
import com.stockmonitor.engine.BacktestEngine;
import com.stockmonitor.model.Backtest;
import com.stockmonitor.repository.BacktestRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for backtest execution and storage (T177).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

  // TODO: TECH_DEBT_001 - Refactor to use database persistence instead of in-memory storage
  // This works but creates maintenance burden. See backlog item TECH_DEBT_001.
  private final BacktestEngine backtestEngine;
  private final BacktestRepository backtestRepository;

  /**
   * Start backtest (async).
   */
  public BacktestDTO startBacktest(
      UUID portfolioId, LocalDate startDate, LocalDate endDate, BacktestConstraintsDTO constraints) {
    log.info("Starting backtest for portfolio: {}", portfolioId);

    BacktestDTO result = backtestEngine.runBacktest(portfolioId, startDate, endDate, constraints);

    return result;
  }

  /**
   * Get backtest results from database.
   */
  @Transactional(readOnly = true)
  public BacktestDTO getBacktest(UUID backtestId) {
    log.info("Getting backtest results for ID: {}", backtestId);

    return backtestRepository.findById(backtestId)
        .map(this::convertToDTO)
        .orElse(null);
  }

  /**
   * Convert Backtest entity to DTO.
   */
  private BacktestDTO convertToDTO(Backtest backtest) {
    return BacktestDTO.builder()
        .backtestId(backtest.getId())
        .portfolioId(null) // Portfolio ID not stored in Backtest entity
        .status(backtest.getStatus())
        .startDate(backtest.getStartDate())
        .endDate(backtest.getEndDate())
        .cagr(backtest.getCagrPct())
        .sharpeRatio(backtest.getSharpeRatio())
        .maxDrawdown(backtest.getMaxDrawdownPct())
        .equityCurve(java.util.Collections.emptyList()) // TODO: Parse from JSON column
        .averageTurnover(backtest.getAvgTurnoverPct())
        .tradeCount(0) // TODO: Calculate from trade history (currently stubbed as 0)
        .benchmarkCAGR(backtest.getBenchmarkReturnPct())
        .beatEqualWeight(backtest.getBeatEqualWeight())
        .totalTransactionCosts(backtest.getTotalCostBps())
        .build();
  }
}
