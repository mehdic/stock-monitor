package com.stockmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.dto.BacktestConstraintsDTO;
import com.stockmonitor.dto.BacktestDTO;
import com.stockmonitor.engine.BacktestEngine;
import com.stockmonitor.model.Backtest;
import com.stockmonitor.model.BacktestStatus;
import com.stockmonitor.repository.BacktestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for backtest execution and storage (T177).
 *
 * <p>Implements async job queue pattern to prevent HTTP thread blocking during long-running backtest
 * operations (5-120s when fully implemented).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

  private final BacktestEngine backtestEngine;
  private final BacktestRepository backtestRepository;
  private final ObjectMapper objectMapper;

  /**
   * Start a backtest asynchronously.
   *
   * <p>Creates backtest record with PENDING status, starts async processing, and returns immediately.
   * Client polls GET /api/backtests/{id} for results.
   *
   * @return Backtest entity with status PENDING
   */
  @Transactional
  public Backtest startBacktest(
      UUID portfolioId,
      UUID userId,
      UUID universeId,
      UUID constraintSetId,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      BacktestConstraintsDTO constraints) {
    log.info("Creating backtest for portfolio: {}", portfolioId);

    // Save initial record with PENDING status
    Backtest backtest = Backtest.builder()
        .userId(userId)
        .portfolioId(portfolioId)
        .universeId(universeId)
        .constraintSetId(constraintSetId)
        .name(name)
        .startDate(startDate)
        .endDate(endDate)
        .status(BacktestStatus.PENDING)
        .initialCapital(new java.math.BigDecimal("1000000.00")) // Default initial capital
        .equityCurveData("[]")
        .turnoverHistory("[]")
        .costAssumptions("{}")
        .build();

    // Store constraints as JSON for async processing
    try {
      String constraintsJson = objectMapper.writeValueAsString(constraints);
      backtest.setConstraintsJson(constraintsJson);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize constraints", e);
      throw new IllegalArgumentException("Invalid constraints", e);
    }

    backtest = backtestRepository.save(backtest);

    // Start async processing (fire and forget)
    executeBacktestAsync(backtest.getId(), portfolioId, startDate, endDate, constraints);

    return backtest;
  }

  /**
   * Execute backtest in background thread.
   *
   * <p>Updates database record when complete/failed. Runs in separate thread pool to avoid blocking
   * HTTP threads.
   */
  @Async("backtestExecutor")
  public void executeBacktestAsync(
      UUID backtestId,
      UUID portfolioId,
      LocalDate startDate,
      LocalDate endDate,
      BacktestConstraintsDTO constraints) {

    log.info("Starting async backtest execution: {}", backtestId);
    LocalDateTime startTime = LocalDateTime.now();

    try {
      // Update status to RUNNING
      Backtest backtest =
          backtestRepository
              .findById(backtestId)
              .orElseThrow(
                  () -> new IllegalStateException("Backtest not found: " + backtestId));

      backtest.setStatus(BacktestStatus.RUNNING);
      backtest.setStartedAt(startTime);
      backtestRepository.save(backtest);

      // Execute backtest (long-running operation, currently stub)
      BacktestDTO result = backtestEngine.runBacktest(portfolioId, startDate, endDate, constraints);

      // Update with results
      backtest.setStatus(BacktestStatus.COMPLETED);
      backtest.setCompletedAt(LocalDateTime.now());
      backtest.setExecutionDurationMs(
          java.time.Duration.between(startTime, LocalDateTime.now()).toMillis());
      backtest.setCagrPct(result.getCagr());
      backtest.setSharpeRatio(result.getSharpeRatio());
      backtest.setMaxDrawdownPct(result.getMaxDrawdown());
      backtest.setTotalReturnPct(result.getCagr()); // Using CAGR as total return for now
      backtest.setAvgTurnoverPct(result.getAverageTurnover());
      backtest.setBenchmarkReturnPct(result.getBenchmarkCAGR());
      backtest.setBeatEqualWeight(result.getBeatEqualWeight());
      backtest.setTotalCostBps(result.getTotalTransactionCosts());

      backtestRepository.save(backtest);

      log.info("Backtest completed successfully: {} (duration: {}ms)",
          backtestId, backtest.getExecutionDurationMs());

    } catch (Exception e) {
      log.error("Backtest failed: " + backtestId, e);

      // Update status to FAILED
      Backtest backtest = backtestRepository.findById(backtestId).orElse(null);
      if (backtest != null) {
        backtest.setStatus(BacktestStatus.FAILED);
        backtest.setCompletedAt(LocalDateTime.now());
        backtest.setExecutionDurationMs(
            java.time.Duration.between(startTime, LocalDateTime.now()).toMillis());
        backtest.setErrorMessage(e.getMessage());
        backtestRepository.save(backtest);
      }
    }
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
        .portfolioId(backtest.getPortfolioId())
        .status(backtest.getStatus() != null ? backtest.getStatus().name() : "PENDING")
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
        .errorMessage(backtest.getErrorMessage())
        .build();
  }
}
