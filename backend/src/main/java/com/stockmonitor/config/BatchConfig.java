package com.stockmonitor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch configuration for scheduled jobs and background processing.
 *
 * Batch Jobs:
 * - Month-end recommendation runs (T-3, T-1, T scheduling)
 * - Data ingestion jobs (prices, fundamentals, estimates)
 * - Report generation jobs
 * - Audit log archival jobs
 *
 * Configuration:
 * - Job repository: PostgreSQL-backed for persistence (auto-configured by Spring Boot 3.x)
 * - Transaction manager: DataSource-based for ACID compliance
 * - Job launcher: Async execution to avoid blocking
 * - Task executor: Simple async for parallel step execution
 *
 * Note: @EnableBatchProcessing is NOT used in Spring Boot 3.x as it disables auto-configuration.
 * Spring Boot 3.x auto-configures JobRepository and schema initialization automatically.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true", matchIfMissing = true)
public class BatchConfig {

    private final DataSource dataSource;

    /**
     * Transaction manager for batch jobs.
     * Uses the same data source as application for consistency.
     * Named 'transactionManager' for Spring Batch JobRepository compatibility.
     */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Task executor for async job execution.
     * Allows batch jobs to run without blocking the main thread.
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(10); // Limit concurrent batch jobs
        return taskExecutor;
    }

    /**
     * Job launcher with async execution support.
     * Enables non-blocking job launches for scheduled tasks.
     */
    @Bean(name = "asyncJobLauncher")
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
