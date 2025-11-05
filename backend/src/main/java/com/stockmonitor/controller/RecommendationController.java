package com.stockmonitor.controller;

import com.stockmonitor.dto.ExclusionDTO;
import com.stockmonitor.dto.RecommendationDTO;
import com.stockmonitor.dto.RecommendationRunDTO;
import com.stockmonitor.dto.TriggerRunRequest;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UserRepository;
import jakarta.validation.Valid;
import com.stockmonitor.service.ExclusionExportService;
import com.stockmonitor.service.ExclusionReasonService;
import com.stockmonitor.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Recommendation controller handling recommendation generation and retrieval.
 *
 * <p>Endpoints:
 * - POST /api/runs - Trigger new recommendation run (OWNER only for manual runs)
 * - GET /api/runs/{id} - Get run details
 * - GET /api/runs/{id}/recommendations - Get recommendations for a run
 * - GET /api/users/{userId}/runs - Get all runs for a user
 *
 * <p>Run Types per FR-028:
 * - SCHEDULED: Official month-end runs (SERVICE role only)
 * - OFF_CYCLE: Manual test runs (OWNER role only)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final ExclusionReasonService exclusionReasonService;
  private final ExclusionExportService exclusionExportService;
  private final UserRepository userRepository;
  private final PortfolioRepository portfolioRepository;

  /**
   * Trigger new recommendation run.
   *
   * <p>Role validation per FR-062 and T072.5:
   * - OWNER role: Can trigger manual (OFF_CYCLE) runs
   * - SERVICE role: Can trigger SCHEDULED runs only
   * - VIEWER role: Blocked with 403 Forbidden
   *
   * @param request Request containing portfolio ID, optional universe ID, and optional run type
   * @return Created run details
   */
  @PostMapping("/api/runs")
  @PreAuthorize("hasRole('OWNER') or hasRole('SERVICE')")
  public ResponseEntity<RecommendationRunDTO> triggerRun(@Valid @RequestBody TriggerRunRequest request) {

    log.info("Trigger recommendation run request: portfolio={}, universe={}, runType={}",
        request.getPortfolioId(), request.getUniverseId(), request.getRunType());

    // Get portfolio to determine universe if not provided
    Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
        .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

    // Use provided universe or portfolio's active universe
    UUID universeId = request.getUniverseId() != null
        ? request.getUniverseId()
        : portfolio.getActiveUniverseId();

    if (universeId == null) {
      throw new IllegalArgumentException("Universe ID must be provided or portfolio must have an active universe");
    }

    // Default to OFF_CYCLE for manual runs to avoid overwriting scheduled results
    String runType = request.getRunType();
    if (runType == null || runType.trim().isEmpty()) {
      runType = "OFF_CYCLE";
    }

    RecommendationRunDTO run =
        recommendationService.triggerRecommendationRun(request.getPortfolioId(), universeId, runType);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(run);
  }

  @GetMapping("/api/runs/{id}")
  public ResponseEntity<RecommendationRunDTO> getRecommendationRun(@PathVariable UUID id) {
    log.info("Get recommendation run request for ID: {}", id);
    RecommendationRunDTO run = recommendationService.getRecommendationRun(id);
    return ResponseEntity.ok(run);
  }

  @GetMapping("/api/runs/{id}/recommendations")
  public ResponseEntity<List<RecommendationDTO>> getRecommendations(@PathVariable UUID id) {
    log.info("Get recommendations request for run ID: {}", id);
    List<RecommendationDTO> recommendations =
        recommendationService.getRecommendationsForRun(id);
    return ResponseEntity.ok(recommendations);
  }

  @GetMapping("/api/users/{userId}/runs")
  public ResponseEntity<List<RecommendationRunDTO>> getRunsForUser(@PathVariable UUID userId) {
    log.info("Get all runs request for user: {}", userId);
    List<RecommendationRunDTO> runs = recommendationService.getRecommendationRunsForUser(userId);
    return ResponseEntity.ok(runs);
  }

  /**
   * Get current recommendations for a portfolio.
   * Returns recommendations from most recent SCHEDULED run only (not OFF_CYCLE runs).
   * Per FR-028, off-cycle runs don't overwrite official recommendations.
   *
   * @param portfolioId Portfolio ID
   * @return List of recommendations from latest SCHEDULED run
   */
  @GetMapping("/api/portfolios/{portfolioId}/recommendations")
  public ResponseEntity<List<RecommendationDTO>> getCurrentRecommendationsForPortfolio(
      @PathVariable UUID portfolioId) {
    log.info("Get current recommendations for portfolio: {}", portfolioId);
    List<RecommendationDTO> recommendations =
        recommendationService.getCurrentRecommendationsForPortfolio(portfolioId);
    return ResponseEntity.ok(recommendations);
  }

  /**
   * Get all runs, optionally filtered by run_type.
   * Per FR-028, supports filtering by SCHEDULED or OFF_CYCLE.
   *
   * @param runType Optional filter: SCHEDULED or OFF_CYCLE
   * @return List of runs
   */
  @GetMapping("/api/runs")
  public ResponseEntity<List<RecommendationRunDTO>> getAllRuns(
      @RequestParam(required = false) String runType) {
    log.info("Get runs request with run_type filter: {}", runType);

    // Get authenticated user's ID
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = authentication.getName();
    UUID userId = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new IllegalStateException("User not found"))
        .getId();

    List<RecommendationRunDTO> runs;
    if (runType != null && !runType.trim().isEmpty()) {
      runs = recommendationService.getRunsByTypeForUser(userId, runType.toUpperCase());
    } else {
      runs = recommendationService.getRecommendationRunsForUser(userId);
    }

    return ResponseEntity.ok(runs);
  }

  /**
   * Get exclusions for a run (T195, FR-031).
   */
  @GetMapping("/api/runs/{id}/exclusions")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<List<ExclusionDTO>> getExclusions(@PathVariable UUID id) {
    log.info("Get exclusions for run: {}", id);
    List<ExclusionDTO> exclusions = exclusionReasonService.getExclusionsForRun(id);
    return ResponseEntity.ok(exclusions);
  }

  /**
   * Export exclusions as CSV (T196, FR-032).
   */
  @GetMapping("/api/runs/{id}/exclusions/export")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<String> exportExclusions(@PathVariable UUID id) {
    log.info("Export exclusions for run: {}", id);
    String csv = exclusionExportService.exportToCsv(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.set("Content-Disposition", "attachment; filename=exclusions-" + id + ".csv");

    return ResponseEntity.ok().headers(headers).body(csv);
  }
}
