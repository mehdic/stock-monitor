package com.stockmonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.dto.RecommendationDTO;
import com.stockmonitor.dto.RecommendationRunDTO;
import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import com.stockmonitor.service.DataSourceHealthService.DataHealthResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration service for recommendation workflow.
 *
 * <p>Manages the end-to-end process of generating, storing, and retrieving recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

  private final RecommendationEngine recommendationEngine;
  private final DataSourceHealthService dataSourceHealthService;
  private final RecommendationRunRepository recommendationRunRepository;
  private final RecommendationRepository recommendationRepository;
  private final UniverseRepository universeRepository;
  private final ConstraintSetRepository constraintSetRepository;
  private final PortfolioRepository portfolioRepository;
  private final ObjectMapper objectMapper;

  /**
   * Trigger a new recommendation run for a portfolio.
   *
   * @param portfolioId Portfolio ID
   * @param universeId Universe to use for recommendations
   * @param runType Run type: SCHEDULED (official month-end) or OFF_CYCLE (manual test run)
   * @return Created RecommendationRunDTO
   */
  @Transactional
  public RecommendationRunDTO triggerRecommendationRun(UUID portfolioId, UUID universeId, String runType) {
    log.info("Triggering {} recommendation run for portfolio {} with universe {}",
        runType, portfolioId, universeId);

    // 1. Get portfolio
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

    // 2. Get universe
    Universe universe =
        universeRepository
            .findById(universeId)
            .orElseThrow(() -> new IllegalArgumentException("Universe not found"));

    // 3. Get active constraints for user
    ConstraintSet constraints =
        constraintSetRepository
            .findByUserIdAndIsActiveTrue(portfolio.getUserId())
            .orElseThrow(() -> new IllegalStateException("No active constraint set found for user"));

    // 4. Check data health
    DataHealthResult healthCheck = dataSourceHealthService.checkDataHealth(24);

    // 5. Validate and default run type
    if (runType == null || runType.trim().isEmpty()) {
      runType = "OFF_CYCLE"; // Default to off-cycle to avoid overwriting scheduled runs
    }

    // Normalize run type
    runType = runType.toUpperCase();
    if (!runType.equals("SCHEDULED") && !runType.equals("OFF_CYCLE")) {
      throw new IllegalArgumentException("Invalid run type. Must be SCHEDULED or OFF_CYCLE");
    }

    // 6. Create run record
    String dataFreshnessSnapshot;
    try {
      dataFreshnessSnapshot = objectMapper.writeValueAsString(healthCheck);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize health check to JSON, using summary only", e);
      dataFreshnessSnapshot = healthCheck.summary();
    }

    RecommendationRun run =
        RecommendationRun.builder()
            .userId(portfolio.getUserId())
            .portfolioId(portfolioId)
            .universeId(universeId)
            .constraintSetId(constraints.getId())
            .runType(runType)
            .status("RUNNING")
            .scheduledDate(LocalDate.now())
            .startedAt(LocalDateTime.now())
            .dataFreshnessCheckPassed(healthCheck.healthy())
            .dataFreshnessSnapshot(dataFreshnessSnapshot)
            .constraintFeasibilityCheckPassed(true) // Placeholder
            .build();

    RecommendationRun savedRun = recommendationRunRepository.save(run);

    // 6. Generate recommendations (async in production)
    try {
      List<Recommendation> recommendations =
          recommendationEngine.generateRecommendations(savedRun, universe, constraints, portfolio);

      // 7. Update run with results
      savedRun.setStatus("COMPLETED");
      savedRun.setCompletedAt(LocalDateTime.now());
      savedRun.setExecutionDurationMs(
          java.time.Duration.between(savedRun.getStartedAt(), LocalDateTime.now()).toMillis());
      savedRun.setRecommendationCount(recommendations.size());

      // Calculate aggregate metrics (only if recommendations exist)
      if (!recommendations.isEmpty()) {
        BigDecimal avgAlpha =
            recommendations.stream()
                .map(Recommendation::getExpectedAlphaBps)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                    BigDecimal.valueOf(recommendations.size()), 2, java.math.RoundingMode.HALF_UP);
        savedRun.setExpectedAlphaBps(avgAlpha);

        BigDecimal avgCost =
            recommendations.stream()
                .map(Recommendation::getExpectedCostBps)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                    BigDecimal.valueOf(recommendations.size()), 2, java.math.RoundingMode.HALF_UP);
        savedRun.setEstimatedCostBps(avgCost);
      } else {
        // No recommendations - set defaults
        savedRun.setExpectedAlphaBps(BigDecimal.ZERO);
        savedRun.setEstimatedCostBps(BigDecimal.ZERO);
      }

      savedRun.setDecision("PENDING"); // User needs to review

      recommendationRunRepository.save(savedRun);

      log.info(
          "Recommendation run {} completed successfully with {} recommendations",
          savedRun.getId(),
          recommendations.size());

    } catch (Exception e) {
      log.error("Error generating recommendations for run {}", savedRun.getId(), e);
      savedRun.setStatus("FAILED");
      savedRun.setErrorMessage(e.getMessage());
      savedRun.setCompletedAt(LocalDateTime.now());
      recommendationRunRepository.save(savedRun);
      throw new RuntimeException("Failed to generate recommendations", e);
    }

    return RecommendationRunDTO.from(savedRun);
  }

  /**
   * Get recommendations for a specific run.
   *
   * @param runId Recommendation run ID
   * @return List of recommendations ordered by rank
   */
  @Transactional(readOnly = true)
  public List<RecommendationDTO> getRecommendationsForRun(UUID runId) {
    log.info("Fetching recommendations for run {}", runId);

    // Verify run exists
    recommendationRunRepository
        .findById(runId)
        .orElseThrow(() -> new IllegalArgumentException("Recommendation run not found"));

    return recommendationRepository.findByRunIdOrderByRankAsc(runId).stream()
        .map(RecommendationDTO::from)
        .collect(Collectors.toList());
  }

  /**
   * Get recommendation run details.
   *
   * @param runId Run ID
   * @return RecommendationRunDTO
   */
  @Transactional(readOnly = true)
  public RecommendationRunDTO getRecommendationRun(UUID runId) {
    log.info("Fetching recommendation run {}", runId);
    RecommendationRun run =
        recommendationRunRepository
            .findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation run not found"));
    return RecommendationRunDTO.from(run);
  }

  /**
   * Get all recommendation runs for a user.
   *
   * @param userId User ID
   * @return List of runs ordered by created date descending
   */
  @Transactional(readOnly = true)
  public List<RecommendationRunDTO> getRecommendationRunsForUser(UUID userId) {
    log.info("Fetching recommendation runs for user {}", userId);
    return recommendationRunRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(RecommendationRunDTO::from)
        .collect(Collectors.toList());
  }

  /**
   * Get current (most recent SCHEDULED) recommendations for a portfolio.
   * Used per FR-028 to isolate off-cycle runs from official recommendations.
   *
   * @param portfolioId Portfolio ID
   * @return List of recommendations from most recent SCHEDULED run
   */
  @Transactional(readOnly = true)
  public List<RecommendationDTO> getCurrentRecommendationsForPortfolio(UUID portfolioId) {
    log.info("Fetching current recommendations for portfolio {}", portfolioId);

    // Find most recent SCHEDULED run for portfolio
    List<RecommendationRun> scheduledRuns =
        recommendationRunRepository.findByPortfolioIdAndRunTypeOrderByCreatedAtDesc(
            portfolioId, "SCHEDULED");

    if (scheduledRuns.isEmpty()) {
      log.info("No SCHEDULED runs found for portfolio {}", portfolioId);
      return List.of();
    }

    RecommendationRun latestScheduled = scheduledRuns.get(0);
    log.info("Returning recommendations from SCHEDULED run {}", latestScheduled.getId());
    return getRecommendationsForRun(latestScheduled.getId());
  }

  /**
   * Get recommendation runs filtered by run type.
   *
   * @param userId User ID
   * @param runType SCHEDULED or OFF_CYCLE
   * @return List of runs matching the type
   */
  @Transactional(readOnly = true)
  public List<RecommendationRunDTO> getRunsByTypeForUser(UUID userId, String runType) {
    log.info("Fetching {} runs for user {}", runType, userId);
    return recommendationRunRepository.findByUserIdAndRunTypeOrderByCreatedAtDesc(userId, runType)
        .stream()
        .map(RecommendationRunDTO::from)
        .collect(Collectors.toList());
  }
}
