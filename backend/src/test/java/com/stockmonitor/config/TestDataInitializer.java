package com.stockmonitor.config;

import com.stockmonitor.helper.TestDataHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Initializes global test data (universes, factor models) that all tests can use.
 * Runs once when the test context is loaded.
 */
@Slf4j
@Configuration
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer {

  private final TestDataHelper testDataHelper;

  @PostConstruct
  public void initializeTestData() {
    log.info("Initializing global test data...");

    // Create default universes
    testDataHelper.createTestUniverse("S&P 500");
    testDataHelper.createTestUniverse("Russell 2000");
    testDataHelper.createTestUniverse("Custom Test Universe");

    // Create default factor model version
    testDataHelper.createTestFactorModelVersion("1.0.0");

    log.info("Global test data initialized successfully");
  }
}
