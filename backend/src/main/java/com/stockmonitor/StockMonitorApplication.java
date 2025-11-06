package com.stockmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for StockMonitor.
 *
 * Month-End Market Analyst - Financial market monitoring and prediction system.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
@EnableAsync
public class StockMonitorApplication {

  public static void main(String[] args) {
    SpringApplication.run(StockMonitorApplication.class, args);
  }
}
