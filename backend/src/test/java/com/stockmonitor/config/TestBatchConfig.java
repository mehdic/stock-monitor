package com.stockmonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.repository.NotificationRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.NotificationService;
import com.stockmonitor.service.WebSocketNotificationService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Test configuration for batch-test profile.
 * Provides stub batch jobs and mocks for dependencies needed by MonthEndScheduler.
 */
@TestConfiguration
@Profile("batch-test")
public class TestBatchConfig {

    /**
     * Stub pre-compute batch job for testing.
     * Does nothing but allows MonthEndScheduler to run without errors.
     */
    @Bean("preComputeJob")
    public Job preComputeJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Step step = new StepBuilder("preComputeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // Stub implementation - does nothing
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();

        return new JobBuilder("preComputeJob", jobRepository)
                .start(step)
                .build();
    }

    /**
     * Stub staging batch job for testing.
     * Does nothing but allows MonthEndScheduler to run without errors.
     */
    @Bean("stagingJob")
    public Job stagingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Step step = new StepBuilder("stagingStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // Stub implementation - does nothing
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();

        return new JobBuilder("stagingJob", jobRepository)
                .start(step)
                .build();
    }

    /**
     * Stub finalization batch job for testing.
     * Does nothing but allows MonthEndScheduler to run without errors.
     */
    @Bean("finalizationJob")
    public Job finalizationJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Step step = new StepBuilder("finalizationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // Stub implementation - does nothing
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();

        return new JobBuilder("finalizationJob", jobRepository)
                .start(step)
                .build();
    }

    /**
     * Synchronous job launcher for testing.
     * Uses SyncTaskExecutor to run jobs synchronously in tests for predictable behavior.
     */
    @Bean("jobLauncher")
    @Primary
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor()); // Synchronous for tests
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * Mock WebSocketNotificationService for testing.
     * NotificationService requires this dependency.
     */
    @MockBean
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * Provide ObjectMapper for NotificationService.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Real NotificationService with real repositories for integration testing.
     * Uses mocked WebSocketNotificationService to avoid WebSocket setup.
     */
    @Bean
    @Primary
    public NotificationService notificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            WebSocketNotificationService webSocketNotificationService,
            ObjectMapper objectMapper
    ) {
        return new NotificationService(
                notificationRepository,
                userRepository,
                webSocketNotificationService,
                objectMapper
        );
    }
}
