package com.stockmonitor.scheduler;

import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import com.stockmonitor.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Month-End scheduler for automated recommendation runs per FR-020, FR-021, FR-022.
 *
 * Executes three-stage month-end workflow:
 * - T-3: Pre-compute stage (3 days before month-end at 01:00 UTC)
 * - T-1: Staging stage (1 day before month-end at 01:00 UTC)
 * - T: Finalization stage (on month-end date at 01:00 UTC)
 *
 * Implements idempotency checks to prevent duplicate runs.
 * Sends notifications at each stage per FR-045.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true", matchIfMissing = true)
public class MonthEndScheduler {

    private final RecommendationRunRepository runRepository;
    private final PortfolioRepository portfolioRepository;
    private final NotificationService notificationService;
    private final JobLauncher jobLauncher;
    private final Job preComputeBatchJob;
    private final Job stagingBatchJob;
    private final Job finalizationBatchJob;

    public MonthEndScheduler(
            RecommendationRunRepository runRepository,
            PortfolioRepository portfolioRepository,
            NotificationService notificationService,
            @Qualifier("jobLauncher") JobLauncher jobLauncher,
            @Qualifier("preComputeJob") Job preComputeBatchJob,
            @Qualifier("stagingJob") Job stagingBatchJob,
            @Qualifier("finalizationJob") Job finalizationBatchJob) {
        this.runRepository = runRepository;
        this.portfolioRepository = portfolioRepository;
        this.notificationService = notificationService;
        this.jobLauncher = jobLauncher;
        this.preComputeBatchJob = preComputeBatchJob;
        this.stagingBatchJob = stagingBatchJob;
        this.finalizationBatchJob = finalizationBatchJob;
    }

    /**
     * T-3 Pre-compute job: Create scheduled runs for all portfolios.
     * Runs 3 days before month-end at 01:00 UTC.
     * Cron: 0 0 1 L-3 * ? (L-3 = 3 days before last day of month)
     */
    @Scheduled(cron = "0 0 1 L-3 * ?", zone = "UTC")
    public void executeT3PreCompute() {
        log.info("Starting T-3 pre-compute job for month-end recommendations");

        LocalDate monthEndDate = getCurrentMonthEndDate();
        LocalDateTime scheduledFor = monthEndDate.atTime(1, 0);

        // Check idempotency: Don't create duplicate runs for this month-end
        List<RecommendationRun> existingRuns = runRepository.findByScheduledDateAndRunType(monthEndDate, "SCHEDULED");
        if (!existingRuns.isEmpty()) {
            log.info("T-3 pre-compute already executed for month-end {}. Skipping.", monthEndDate);
            return;
        }

        // Create scheduled runs for all active portfolios
        List<Portfolio> portfolios = portfolioRepository.findAll();
        log.info("Found {} portfolios for month-end {}", portfolios.size(), monthEndDate);
        int createdCount = 0;

        for (Portfolio portfolio : portfolios) {
            // Skip portfolios without active universe or constraint set
            if (portfolio.getActiveUniverseId() == null || portfolio.getActiveConstraintSetId() == null) {
                log.info("Skipping portfolio {} - no active universe ({}==null? {}) or constraint set ({}==null? {})",
                        portfolio.getId(),
                        portfolio.getActiveUniverseId(),
                        portfolio.getActiveUniverseId() == null,
                        portfolio.getActiveConstraintSetId(),
                        portfolio.getActiveConstraintSetId() == null);
                continue;
            }

            try {
                RecommendationRun run = RecommendationRun.builder()
                        .userId(portfolio.getUserId())
                        .universeId(portfolio.getActiveUniverseId())
                        .constraintSetId(portfolio.getActiveConstraintSetId())
                        // TODO: Add factorModelVersionId to RecommendationRun model if needed
                        .runType("SCHEDULED")
                        .status("SCHEDULED")
                        .scheduledDate(scheduledFor.toLocalDate())
                        .dataFreshnessCheckPassed(false)
                        .build();

                run = runRepository.save(run);
                createdCount++;

                log.info("Created scheduled run {} for portfolio {} (user: {})", run.getId(), portfolio.getId(), portfolio.getUserId());

                // Send T-3 notification to user
                notificationService.sendT3PreComputeNotification(portfolio.getUserId(), run.getId());

                log.info("Sent T-3 notification for run {}", run.getId());
            } catch (Exception e) {
                log.error("Failed to create scheduled run for portfolio {}: {}", portfolio.getId(), e.getMessage(), e);
            }
        }

        log.info("T-3 pre-compute completed. Created {} scheduled runs for month-end {}", createdCount, monthEndDate);

        // Trigger Spring Batch job for pre-compute
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("monthEndDate", monthEndDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(preComputeBatchJob, params);
        } catch (Exception e) {
            log.error("Failed to launch pre-compute batch job: {}", e.getMessage(), e);
        }
    }

    /**
     * T-1 Staging job: Perform data freshness checks and transition to STAGED.
     * Runs 1 day before month-end at 01:00 UTC.
     * Cron: 0 0 1 L-1 * ? (L-1 = 1 day before last day of month)
     */
    @Scheduled(cron = "0 0 1 L-1 * ?", zone = "UTC")
    public void executeT1Staging() {
        log.info("Starting T-1 staging job for month-end recommendations");

        LocalDate monthEndDate = getCurrentMonthEndDate();
        LocalDateTime scheduledFor = monthEndDate.atTime(1, 0);

        // Find all SCHEDULED runs for this month-end
        List<RecommendationRun> scheduledRuns = runRepository.findByScheduledDateAndStatus(monthEndDate, "SCHEDULED");

        if (scheduledRuns.isEmpty()) {
            log.warn("No SCHEDULED runs found for month-end {}. T-3 pre-compute may have failed.", monthEndDate);
            return;
        }

        int stagedCount = 0;
        int failedCount = 0;

        for (RecommendationRun run : scheduledRuns) {
            try {
                // Perform data freshness check per FR-025
                boolean dataFresh = performDataFreshnessCheck(run);

                // Update run status to STAGED
                run.setStatus("STAGED");
                run.setDataFreshnessCheckPassed(dataFresh);
                // TODO: Add dataFreshnessSnapshot field to RecommendationRun model if needed
                // run.setDataFreshnessSnapshot(createDataFreshnessSnapshot());

                runRepository.save(run);
                stagedCount++;

                // Send T-1 notification
                notificationService.sendT1StagedNotification(run.getUserId(), run.getId());

                // If data is stale, send warning notification per FR-026
                if (!dataFresh) {
                    notificationService.sendDataStaleNotification(run.getUserId());
                    log.warn("Data freshness check failed for run {}. Notification sent to user.", run.getId());
                }

                log.debug("Staged run {} for portfolio", run.getId());
            } catch (Exception e) {
                log.error("Failed to stage run {}: {}", run.getId(), e.getMessage());
                failedCount++;
            }
        }

        log.info("T-1 staging completed. Staged {} runs, {} failures", stagedCount, failedCount);

        // Trigger Spring Batch job for staging
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("monthEndDate", monthEndDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(stagingBatchJob, params);
        } catch (Exception e) {
            log.error("Failed to launch staging batch job: {}", e.getMessage(), e);
        }
    }

    /**
     * T Finalization job: Execute optimization and finalize runs.
     * Runs on month-end date at 01:00 UTC.
     * Cron: 0 0 1 L * ? (L = last day of month)
     */
    @Scheduled(cron = "0 0 1 L * ?", zone = "UTC")
    public void executeT0Finalization() {
        log.info("Starting T finalization job for month-end recommendations");

        LocalDate monthEndDate = getCurrentMonthEndDate();
        LocalDateTime scheduledFor = monthEndDate.atTime(1, 0);

        // Find all STAGED runs for this month-end
        List<RecommendationRun> stagedRuns = runRepository.findByScheduledDateAndStatus(monthEndDate, "STAGED");

        if (stagedRuns.isEmpty()) {
            log.warn("No STAGED runs found for month-end {}. T-1 staging may have failed.", monthEndDate);
            return;
        }

        int finalizedCount = 0;
        int failedCount = 0;

        for (RecommendationRun run : stagedRuns) {
            try {
                // Execute optimization and generate recommendations
                // (Actual recommendation engine execution happens in batch job)

                // Update run status to FINALIZED
                run.setStatus("FINALIZED");
                run.setCompletedAt(LocalDateTime.now());

                runRepository.save(run);
                finalizedCount++;

                // Send T finalization notification (HIGH priority per FR-045)
                notificationService.sendTFinalizedNotification(run.getUserId(), run.getId());

                log.info("Finalized run {} for user {}", run.getId(), run.getUserId());
            } catch (Exception e) {
                log.error("Failed to finalize run {}: {}", run.getId(), e.getMessage());
                run.setStatus("FAILED");
                runRepository.save(run);
                failedCount++;
            }
        }

        log.info("T finalization completed. Finalized {} runs, {} failures", finalizedCount, failedCount);

        // Trigger Spring Batch job for finalization
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("monthEndDate", monthEndDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(finalizationBatchJob, params);
        } catch (Exception e) {
            log.error("Failed to launch finalization batch job: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current month-end date.
     *
     * @return Last day of current month
     */
    private LocalDate getCurrentMonthEndDate() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Get latest factor model version ID.
     * TODO: Implement factor model versioning system.
     *
     * @return Factor model version UUID
     */
    private UUID getLatestFactorModelVersion() {
        // Placeholder: Return fixed UUID until factor model versioning implemented
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    /**
     * Perform data freshness check per FR-025.
     * Data is considered fresh if updated within last 48 hours.
     *
     * TODO: This is a PLACEHOLDER implementation that always returns true.
     * Production deployment requires implementation of actual checks for:
     * - Market data (prices, volumes) - must be < 48 hours old
     * - Factor scores - must be < 48 hours old
     * - Universe constituents - must be current
     * - Constraint parameters - must be current
     *
     * @param run Recommendation run to check
     * @return True if data is fresh (CURRENTLY ALWAYS RETURNS TRUE)
     */
    private boolean performDataFreshnessCheck(RecommendationRun run) {
        log.debug("Performing data freshness check for run {}", run.getId());
        return true;  // Placeholder - always returns true
    }

    /**
     * Create data freshness snapshot with timestamps per FR-025.
     *
     * @return Data freshness snapshot as JSON-compatible map
     */
    private java.util.Map<String, Object> createDataFreshnessSnapshot() {
        LocalDateTime now = LocalDateTime.now();

        return java.util.Map.of(
                "marketDataAsOf", now.toString(),
                "factorScoresAsOf", now.toString(),
                "universeAsOf", now.toString(),
                "checkedAt", now.toString()
        );
    }
}
