package com.stockmonitor.service;

import com.stockmonitor.model.Recommendation;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Change detection service per FR-027.
 *
 * T113: Compares current recommendations with previous run to detect changes.
 *
 * Change indicators:
 * - NEW: Stock newly recommended (not in previous run)
 * - INCREASED: Target weight increased vs previous run
 * - DECREASED: Target weight decreased vs previous run
 * - UNCHANGED: Target weight same as previous run (within threshold)
 * - REMOVED: Stock was in previous run but not current run
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeDetectionService {

    private final RecommendationRunRepository runRepository;
    private final RecommendationRepository recommendationRepository;

    /**
     * Weight change threshold for UNCHANGED classification (1 basis point = 0.01%)
     */
    private static final BigDecimal WEIGHT_CHANGE_THRESHOLD = new BigDecimal("0.01");

    /**
     * Detect and populate change indicators for a recommendation run.
     *
     * Compares current run with previous FINALIZED run for the same portfolio.
     *
     * @param currentRunId Current run ID
     */
    @Transactional
    public void detectChanges(UUID currentRunId) {
        log.info("Detecting changes for run {}", currentRunId);

        // Fetch current run
        RecommendationRun currentRun = runRepository.findById(currentRunId)
                .orElseThrow(() -> new IllegalArgumentException("Current run not found: " + currentRunId));

        // Find previous FINALIZED run for same user
        Optional<RecommendationRun> previousRunOpt = findPreviousRun(currentRun);

        if (previousRunOpt.isEmpty()) {
            log.info("No previous run found for user {}. Marking all as NEW", currentRun.getUserId());
            markAllAsNew(currentRunId);
            return;
        }

        RecommendationRun previousRun = previousRunOpt.get();
        log.info("Comparing with previous run {} from {}", previousRun.getId(), previousRun.getCompletedAt());

        // Fetch recommendations from both runs
        List<Recommendation> currentRecommendations = recommendationRepository.findByRunId(currentRunId);
        List<Recommendation> previousRecommendations = recommendationRepository.findByRunId(previousRun.getId());

        // Build symbol -> recommendation maps
        Map<String, Recommendation> currentBySymbol = currentRecommendations.stream()
                .collect(Collectors.toMap(Recommendation::getSymbol, r -> r));

        Map<String, Recommendation> previousBySymbol = previousRecommendations.stream()
                .collect(Collectors.toMap(Recommendation::getSymbol, r -> r));

        // Detect changes
        int newCount = 0;
        int increasedCount = 0;
        int decreasedCount = 0;
        int unchangedCount = 0;

        for (Recommendation current : currentRecommendations) {
            String symbol = current.getSymbol();
            String changeIndicator;

            if (!previousBySymbol.containsKey(symbol)) {
                // NEW: Not in previous run
                changeIndicator = "NEW";
                newCount++;
            } else {
                // Compare weights
                Recommendation previous = previousBySymbol.get(symbol);
                BigDecimal weightDiff = current.getTargetWeightPct().subtract(previous.getTargetWeightPct());

                if (weightDiff.abs().compareTo(WEIGHT_CHANGE_THRESHOLD) <= 0) {
                    // UNCHANGED: Weight change within threshold
                    changeIndicator = "UNCHANGED";
                    unchangedCount++;
                } else if (weightDiff.compareTo(BigDecimal.ZERO) > 0) {
                    // INCREASED: Weight increased
                    changeIndicator = "INCREASED";
                    increasedCount++;
                } else {
                    // DECREASED: Weight decreased
                    changeIndicator = "DECREASED";
                    decreasedCount++;
                }
            }

            // Update change indicator
            current.setChangeIndicator(changeIndicator);
            recommendationRepository.save(current);
        }

        // Log REMOVED stocks (in previous but not in current)
        int removedCount = 0;
        for (String symbol : previousBySymbol.keySet()) {
            if (!currentBySymbol.containsKey(symbol)) {
                removedCount++;
                log.debug("Stock {} removed from recommendations", symbol);
            }
        }

        log.info("Change detection complete for run {}: NEW={}, INCREASED={}, DECREASED={}, UNCHANGED={}, REMOVED={}",
                currentRunId, newCount, increasedCount, decreasedCount, unchangedCount, removedCount);
    }

    /**
     * Find previous FINALIZED run for the same user.
     *
     * Returns the most recent FINALIZED run before the current run.
     *
     * @param currentRun Current run
     * @return Previous run, or empty if no previous run exists
     */
    private Optional<RecommendationRun> findPreviousRun(RecommendationRun currentRun) {
        List<RecommendationRun> previousRuns = runRepository.findByUserIdAndStatusOrderByCompletedAtDesc(
                currentRun.getUserId(),
                "FINALIZED"
        );

        // Filter out current run and find most recent
        return previousRuns.stream()
                .filter(run -> !run.getId().equals(currentRun.getId()))
                .filter(run -> run.getCompletedAt() != null)
                .filter(run -> currentRun.getScheduledDate() == null ||
                        run.getCompletedAt().toLocalDate().isBefore(currentRun.getScheduledDate()))
                .findFirst();
    }

    /**
     * Mark all recommendations as NEW (no previous run to compare).
     *
     * @param runId Run ID
     */
    private void markAllAsNew(UUID runId) {
        List<Recommendation> recommendations = recommendationRepository.findByRunId(runId);

        for (Recommendation rec : recommendations) {
            rec.setChangeIndicator("NEW");
        }

        recommendationRepository.saveAll(recommendations);
        log.info("Marked {} recommendations as NEW for run {}", recommendations.size(), runId);
    }

    /**
     * Get change summary for a run.
     *
     * @param runId Run ID
     * @return Map of change indicator -> count
     */
    public Map<String, Integer> getChangeSummary(UUID runId) {
        List<Recommendation> recommendations = recommendationRepository.findByRunId(runId);

        Map<String, Integer> summary = new HashMap<>();
        summary.put("NEW", 0);
        summary.put("INCREASED", 0);
        summary.put("DECREASED", 0);
        summary.put("UNCHANGED", 0);

        for (Recommendation rec : recommendations) {
            String indicator = rec.getChangeIndicator();
            if (indicator != null) {
                summary.put(indicator, summary.getOrDefault(indicator, 0) + 1);
            }
        }

        return summary;
    }
}
