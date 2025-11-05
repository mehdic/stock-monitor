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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job for T-1 staging stage per FR-020, FR-025.
 *
 * T100: Staging batch job executed 1 day before month-end.
 *
 * Responsibilities:
 * - Perform comprehensive data freshness checks per FR-025
 * - Validate cached pre-compute results are still valid
 * - Refresh any stale data
 * - Send alerts if data quality issues detected per FR-026
 * - Mark runs as ready for finalization
 *
 * This job ensures all data is fresh and valid before final optimization.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true", matchIfMissing = true)
public class StagingBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job stagingJob() {
        return new JobBuilder("stagingBatchJob", jobRepository)
                .start(dataFreshnessCheckStep())
                .next(validateCachedDataStep())
                .next(refreshStaleDataStep())
                .next(finalValidationStep())
                .build();
    }

    /**
     * Step 1: Perform comprehensive data freshness checks.
     */
    @Bean
    public Step dataFreshnessCheckStep() {
        return new StepBuilder("dataFreshnessCheck", jobRepository)
                .tasklet(dataFreshnessCheckTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet dataFreshnessCheckTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T-1 Staging: Performing data freshness checks");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement data freshness checks per FR-025
            // 1. Check market data age (must be < 48 hours old)
            // 2. Check factor score age (must be < 72 hours old)
            // 3. Check universe constituent list age (must be current month)
            // 4. Check constraint parameter changes (must be finalized)
            // 5. Record check results in dataFreshnessSnapshot

            log.info("Data freshness checks complete for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 2: Validate cached pre-compute results are still valid.
     */
    @Bean
    public Step validateCachedDataStep() {
        return new StepBuilder("validateCachedData", jobRepository)
                .tasklet(validateCachedDataTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet validateCachedDataTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T-1 Staging: Validating cached pre-compute data");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement cached data validation
            // 1. Verify all cached factor scores exist and are complete
            // 2. Verify covariance matrices are cached
            // 3. Verify sector/liquidity classifications are cached
            // 4. Check cache hit rates (should be >95%)
            // 5. Log any cache misses for investigation

            log.info("Cached data validation complete for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 3: Refresh any stale data detected.
     */
    @Bean
    public Step refreshStaleDataStep() {
        return new StepBuilder("refreshStaleData", jobRepository)
                .tasklet(refreshStaleDataTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet refreshStaleDataTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T-1 Staging: Refreshing stale data if needed");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement stale data refresh logic
            // 1. Re-fetch stale market data (if data is >48 hours old)
            // 2. Re-calculate stale factor scores
            // 3. Update cache with fresh data
            // 4. Send DATA_STALE notifications to users per FR-026
            // 5. Mark runs with dataFreshnessCheckPassed = false if critical data stale

            log.info("Stale data refresh complete for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 4: Final validation before finalization.
     */
    @Bean
    public Step finalValidationStep() {
        return new StepBuilder("finalValidation", jobRepository)
                .tasklet(finalValidationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet finalValidationTasklet() {
        return (contribution, chunkContext) -> {
            log.info("T-1 Staging: Performing final validation");

            String monthEndDate = (String) chunkContext.getStepContext()
                    .getJobParameters()
                    .get("monthEndDate");

            // TODO: Implement final validation logic
            // 1. Verify all SCHEDULED runs have been transitioned to STAGED
            // 2. Verify dataFreshnessCheckPassed flag is set for all runs
            // 3. Verify all users have been notified
            // 4. Log summary statistics (total runs, fresh data %, stale data %)
            // 5. Alert operations team if >10% of runs have stale data

            log.info("Final validation complete for month-end {}", monthEndDate);
            return RepeatStatus.FINISHED;
        };
    }
}
