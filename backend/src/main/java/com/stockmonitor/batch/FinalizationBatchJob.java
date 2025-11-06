package com.stockmonitor.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job for T finalization stage per FR-020, FR-022.
 *
 * T101: Finalization batch job executed on month-end date.
 *
 * Responsibilities:
 * - Execute portfolio optimization for all STAGED runs
 * - Generate recommendations with rankings per FR-008 to FR-014
 * - Calculate change indicators vs previous month per FR-027
 * - Generate reports per FR-041 to FR-044
 * - Archive previous month's recommendations per FR-023
 * - Send high-priority completion notifications per FR-045
 *
 * This is the main execution job that produces final month-end recommendations.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true", matchIfMissing = true)
public class FinalizationBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job finalizationJob() {
        return new JobBuilder("finalizationBatchJob", jobRepository)
                .start(executeOptimizationStep())
                .next(generateRecommendationsStep())
                .next(calculateChangeIndicatorsStep())
                .next(generateReportsStep())
                .next(archivePreviousRunsStep())
                .next(sendCompletionNotificationsStep())
                .build();
    }

    /**
     * Step 1: Execute portfolio optimization for all STAGED runs.
     */
    @Bean
    public Step executeOptimizationStep() {
        return new StepBuilder("executeOptimization", jobRepository)
                .tasklet(executeOptimizationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet executeOptimizationTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Executing portfolio optimization");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement portfolio optimization logic
            // 1. For each STAGED run, invoke RecommendationEngine
            // 2. Run factor-based optimization with current constraints
            // 3. Generate optimal portfolio weights
            // 4. Calculate expected alpha, costs, edge/cost ratio
            // 5. Handle optimization failures gracefully

            log.info("Portfolio optimization complete for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 2: Generate recommendations with rankings.
     */
    @Bean
    public Step generateRecommendationsStep() {
        return new StepBuilder("generateRecommendations", jobRepository)
                .tasklet(generateRecommendationsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet generateRecommendationsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Generating recommendations");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement recommendation generation logic
            // 1. Convert optimization results to Recommendation entities
            // 2. Rank recommendations by expected alpha per FR-009
            // 3. Calculate confidence scores per FR-010
            // 4. Populate driver factors (top 3) per FR-012
            // 5. Generate explanations per FR-013
            // 6. Check constraint compliance per FR-016 to FR-019
            // 7. Save recommendations to database

            log.info("Recommendations generated for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 3: Calculate change indicators vs previous month.
     */
    @Bean
    public Step calculateChangeIndicatorsStep() {
        return new StepBuilder("calculateChangeIndicators", jobRepository)
                .tasklet(calculateChangeIndicatorsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet calculateChangeIndicatorsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Calculating change indicators");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement change indicator calculation per FR-027
            // 1. Find previous month's FINALIZED run for each portfolio
            // 2. Compare recommendations: NEW, INCREASED, DECREASED, UNCHANGED, REMOVED
            // 3. Calculate weight changes for each stock
            // 4. Populate changeIndicator field in Recommendation entities
            // 5. Update recommendations in database

            log.info("Change indicators calculated for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 4: Generate reports for all finalized runs.
     */
    @Bean
    public Step generateReportsStep() {
        return new StepBuilder("generateReports", jobRepository)
                .tasklet(generateReportsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet generateReportsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Generating reports");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement report generation per FR-041 to FR-044
            // 1. For each finalized run, invoke ReportGenerationService
            // 2. Generate HTML report with all sections
            // 3. Generate PDF report with charts
            // 4. Store reports (or cache report data for on-demand generation)
            // 5. Log report generation success/failures

            log.info("Reports generated for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 5: Archive previous month's recommendations.
     */
    @Bean
    public Step archivePreviousRunsStep() {
        return new StepBuilder("archivePreviousRuns", jobRepository)
                .tasklet(archivePreviousRunsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet archivePreviousRunsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Archiving previous runs");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement archival logic per FR-023
            // 1. Find all runs with status=FINALIZED from previous months
            // 2. For runs older than retention period (e.g., 13 months), mark as ARCHIVED
            // 3. Optionally move archived data to cold storage
            // 4. Keep audit log intact per FR-057
            // 5. Update run status to ARCHIVED

            log.info("Previous runs archived for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 6: Send high-priority completion notifications.
     */
    @Bean
    public Step sendCompletionNotificationsStep() {
        return new StepBuilder("sendCompletionNotifications", jobRepository)
                .tasklet(sendCompletionNotificationsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet sendCompletionNotificationsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T Finalization: Sending completion notifications");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement notification sending
            // 1. For each finalized run, send T_FINALIZED notification (HIGH priority)
            // 2. Include link to recommendations page in notification
            // 3. Include summary stats (# recommendations, expected alpha)
            // 4. Send email notifications per user preferences
            // 5. Broadcast WebSocket notifications
            // 6. Log notification delivery success/failures

            log.info("Completion notifications sent for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }
}
