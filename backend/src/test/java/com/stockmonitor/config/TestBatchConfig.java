package com.stockmonitor.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for Spring Batch and WebSocket beans.
 * Provides mock implementations for batch jobs and messaging used by MonthEndScheduler.
 */
@TestConfiguration
public class TestBatchConfig {

    /**
     * Mock JobLauncher that does nothing (batch jobs not executed in tests).
     */
    @Bean
    @Primary
    public JobLauncher jobLauncher() {
        return new JobLauncher() {
            @Override
            public JobExecution run(Job job, JobParameters jobParameters)
                    throws JobExecutionAlreadyRunningException, JobRestartException,
                    JobInstanceAlreadyCompleteException, JobParametersInvalidException {
                // Return a mock JobExecution - batch jobs are not actually executed in tests
                return null;
            }
        };
    }

    /**
     * Mock pre-compute batch job.
     */
    @Bean(name = "preComputeJob")
    public Job preComputeJob() {
        return new Job() {
            @Override
            public String getName() {
                return "preComputeJob";
            }

            @Override
            public boolean isRestartable() {
                return true;
            }

            @Override
            public JobExecution execute(JobParameters parameters) {
                return null;
            }
        };
    }

    /**
     * Mock staging batch job.
     */
    @Bean(name = "stagingJob")
    public Job stagingJob() {
        return new Job() {
            @Override
            public String getName() {
                return "stagingJob";
            }

            @Override
            public boolean isRestartable() {
                return true;
            }

            @Override
            public JobExecution execute(JobParameters parameters) {
                return null;
            }
        };
    }

    /**
     * Mock finalization batch job.
     */
    @Bean(name = "finalizationJob")
    public Job finalizationJob() {
        return new Job() {
            @Override
            public String getName() {
                return "finalizationJob";
            }

            @Override
            public boolean isRestartable() {
                return true;
            }

            @Override
            public JobExecution execute(JobParameters parameters) {
                return null;
            }
        };
    }

    /**
     * Mock SimpMessagingTemplate for WebSocket notifications (disabled in tests).
     */
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }
}
