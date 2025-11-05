package com.stockmonitor.controller;

import com.stockmonitor.dto.ExclusionDTO;
import com.stockmonitor.dto.RecommendationDTO;
import com.stockmonitor.dto.RecommendationRunDTO;
import com.stockmonitor.service.ExclusionExportService;
import com.stockmonitor.service.ExclusionReasonService;
import com.stockmonitor.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  /**
   * Trigger new recommendation run.
   *
   * <p>Role validation per FR-062 and T072.5:
   * - OWNER role: Can trigger manual (OFF_CYCLE) runs
   * - SERVICE role: Can trigger SCHEDULED runs only
   * - VIEWER role: Blocked with 403 Forbidden
   *
   * @param portfolioId Portfolio ID
   * @param universeId Universe ID
   * @param runType SCHEDULED or OFF_CYCLE (defaults to OFF_CYCLE if not specified)
   * @return Created run details
   */
  @PostMapping("/api/runs")
  @PreAuthorize("hasRole('OWNER') or hasRole('SERVICE')")
  public ResponseEntity<RecommendationRunDTO> triggerRun(
      @RequestParam UUID portfolioId,
      @RequestParam UUID universeId,
      @RequestParam(required = false) String runType) {

    log.info("Trigger recommendation run request: portfolio={}, universe={}, runType={}",
        portfolioId, universeId, runType);

    // Default to OFF_CYCLE for manual runs to avoid overwriting scheduled results
    if (runType == null || runType.trim().isEmpty()) {
      runType = "OFF_CYCLE";
    }

    RecommendationRunDTO run =
        recommendationService.triggerRecommendationRun(portfolioId, universeId, runType);

    return ResponseEntity.ok(run);
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
