package com.stockmonitor.controller;

import com.stockmonitor.dto.BacktestConstraintsDTO;
import com.stockmonitor.dto.BacktestDTO;
import com.stockmonitor.model.Backtest;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.repository.BacktestRepository;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.BacktestService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for backtest endpoints (T178).
 *
 * <p>Implements async job queue pattern: POST returns 202 Accepted immediately, client polls GET
 * endpoint for results.
 */
@RestController
@RequestMapping("/api/backtests")
@RequiredArgsConstructor
@Slf4j
public class BacktestController {

  private final BacktestService backtestService;
  private final UserRepository userRepository;
  private final BacktestRepository backtestRepository;
  private final PortfolioRepository portfolioRepository;

  /**
   * POST /api/backtests - Start backtest (returns immediately).
   *
   * <p>Returns 202 Accepted with backtest ID. Client polls GET /api/backtests/{id} for status and
   * results.
   */
  @PostMapping
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<?> runBacktest(@RequestBody BacktestRequest request) {
    log.info("Run backtest request for portfolio: {}", request.getPortfolioId());

    // Validate date range
    if (request.getStartDate().isAfter(request.getEndDate())) {
      return ResponseEntity.badRequest()
          .body(java.util.Map.of("message", "Start date must be before end date"));
    }

    // Get authenticated user ID
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = auth.getName();
    UUID userId =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userEmail))
            .getId();

    // CRITICAL SECURITY FIX: Validate portfolio ownership BEFORE starting backtest
    Portfolio portfolio = portfolioRepository
        .findById(request.getPortfolioId())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            HttpStatus.NOT_FOUND, "Portfolio not found"));

    if (!portfolio.getUserId().equals(userId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You don't have permission to create backtests for this portfolio");
    }

    // Start async backtest (returns immediately with PENDING status)
    Backtest backtest =
        backtestService.startBacktest(
            request.getPortfolioId(),
            userId,
            request.getUniverseId(),
            request.getConstraintSetId(),
            request.getName() != null ? request.getName() : "Backtest",
            request.getStartDate(),
            request.getEndDate(),
            request.getConstraints());

    // Return 202 Accepted (job started, not complete)
    BacktestResponseDTO response = BacktestResponseDTO.fromEntity(backtest);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  /**
   * GET /api/backtests/{id} - Poll for backtest status/results.
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<BacktestDTO> getBacktest(@PathVariable UUID id) {
    log.info("Get backtest results: {}", id);

    // Get authenticated user ID
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = auth.getName();
    UUID userId =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userEmail))
            .getId();

    // Fetch backtest entity for ownership validation
    Backtest backtestEntity = backtestRepository
        .findById(id)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            HttpStatus.NOT_FOUND, "Backtest not found"));

    // CRITICAL SECURITY FIX: Validate ownership
    if (!backtestEntity.getUserId().equals(userId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You don't have permission to access this backtest");
    }

    // Convert to DTO and return
    BacktestDTO backtest = backtestService.getBacktest(id);
    return backtest != null ? ResponseEntity.ok(backtest) : ResponseEntity.notFound().build();
  }

  @Data
  public static class BacktestRequest {
    private UUID portfolioId;
    private UUID universeId;
    private UUID constraintSetId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private BacktestConstraintsDTO constraints;
  }

  /**
   * Response DTO for POST /api/backtests.
   */
  @Data
  @lombok.Builder
  public static class BacktestResponseDTO {
    private UUID backtestId;
    private String status;
    private java.time.LocalDateTime createdAt;
    private String message;

    public static BacktestResponseDTO fromEntity(Backtest backtest) {
      return BacktestResponseDTO.builder()
          .backtestId(backtest.getId())
          .status(backtest.getStatus() != null ? backtest.getStatus().name() : "PENDING")
          .createdAt(backtest.getCreatedAt())
          .message("Backtest started. Poll GET /api/backtests/" + backtest.getId() + " for results.")
          .build();
    }
  }
}
