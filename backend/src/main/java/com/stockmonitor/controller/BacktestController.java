package com.stockmonitor.controller;

import com.stockmonitor.dto.BacktestDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.service.BacktestService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for backtest endpoints (T178).
 */
@RestController
@RequestMapping("/api/backtests")
@RequiredArgsConstructor
@Slf4j
public class BacktestController {

  private final BacktestService backtestService;

  @PostMapping
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<BacktestDTO> runBacktest(@RequestBody BacktestRequest request) {
    log.info("Run backtest request for portfolio: {}", request.getPortfolioId());

    // Validate date range
    if (request.getStartDate().isAfter(request.getEndDate())) {
      return ResponseEntity.badRequest().build();
    }

    BacktestDTO result =
        backtestService.startBacktest(
            request.getPortfolioId(),
            request.getStartDate(),
            request.getEndDate(),
            request.getConstraints());

    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<BacktestDTO> getBacktest(@PathVariable UUID id) {
    log.info("Get backtest results: {}", id);
    BacktestDTO backtest = backtestService.getBacktest(id);
    return backtest != null ? ResponseEntity.ok(backtest) : ResponseEntity.notFound().build();
  }

  @Data
  public static class BacktestRequest {
    private UUID portfolioId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ConstraintSetDTO constraints;
  }
}
