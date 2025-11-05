package com.stockmonitor.service;

import com.stockmonitor.dto.BacktestDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.engine.BacktestEngine;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for backtest execution and storage (T177).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

  private final BacktestEngine backtestEngine;
  private final Map<UUID, BacktestDTO> backtestStore = new ConcurrentHashMap<>();

  /**
   * Start backtest (async).
   */
  @Async
  public BacktestDTO startBacktest(
      UUID portfolioId, LocalDate startDate, LocalDate endDate, ConstraintSetDTO constraints) {
    log.info("Starting backtest for portfolio: {}", portfolioId);

    BacktestDTO result = backtestEngine.runBacktest(portfolioId, startDate, endDate, constraints);
    backtestStore.put(result.getBacktestId(), result);

    return result;
  }

  /**
   * Get backtest results.
   *
   * @param backtestId Backtest ID
   * @return BacktestDTO
   * @throws IllegalArgumentException if backtest not found
   */
  public BacktestDTO getBacktest(UUID backtestId) {
    BacktestDTO result = backtestStore.get(backtestId);
    if (result == null) {
      log.error("Backtest not found: {}", backtestId);
      throw new IllegalArgumentException("Backtest not found: " + backtestId);
    }
    return result;
  }
}
