package com.stockmonitor.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for Spring @Async support.
 *
 * <p>Creates dedicated thread pool for backtest execution to prevent HTTP thread pool exhaustion.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

  /**
   * Thread pool executor for backtest operations.
   *
   * <p>Configuration: - Core pool size: 5 (always running) - Max pool size: 10 (max concurrent
   * backtests) - Queue capacity: 100 (pending jobs) - Rejection policy: CallerRunsPolicy (run in
   * caller thread if pool full)
   *
   * <p>This prevents HTTP thread blocking during long-running backtest operations (5-120s when fully
   * implemented).
   */
  @Bean(name = "backtestExecutor")
  public Executor backtestExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5); // 5 threads always running
    executor.setMaxPoolSize(10); // Max 10 concurrent backtests
    executor.setQueueCapacity(100); // Queue up to 100 pending
    executor.setThreadNamePrefix("backtest-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
