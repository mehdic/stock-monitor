package com.stockmonitor.config;

import com.stockmonitor.helper.TestDataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * Initializes global test data (universes, factor models) that all tests can use.
 * Runs once when the test context is fully loaded and ready.
 */
@Slf4j
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer {

  private final TestDataHelper testDataHelper;
  private volatile boolean initialized = false;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeTestData() {
    // Only initialize once (ApplicationReadyEvent can fire multiple times in tests)
    if (initialized) {
      return;
    }

    synchronized (this) {
      if (initialized) {
        return;
      }

      log.info("Initializing global test data...");

      // Create default universes (each method uses REQUIRES_NEW transaction)
      testDataHelper.createTestUniverse("S&P 500");
      testDataHelper.createTestUniverse("Russell 2000");
      testDataHelper.createTestUniverse("Custom Test Universe");

      // Create default factor model version
      testDataHelper.createTestFactorModelVersion("1.0.0");

      initialized = true;
      log.info("Global test data initialized successfully");
    }
  }
}
