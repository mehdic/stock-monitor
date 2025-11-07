package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.model.DataSource;
import com.stockmonitor.repository.DataSourceRepository;
import com.stockmonitor.service.DataSourceHealthService;
import com.stockmonitor.service.DataSourceHealthService.DataHealthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for data freshness validation and stale data detection.
 *
 * Tests:
 * - Data freshness checks before recommendation runs
 * - Stale data detection across multiple data sources
 * - Health status calculation for active data sources
 * - Freshness threshold enforcement
 */
@Transactional
public class DataFreshnessWorkflowTest extends BaseIntegrationTest {

    @Autowired
    private DataSourceHealthService dataSourceHealthService;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @BeforeEach
    public void setup() {
        // Clean up test data
        dataSourceRepository.deleteAll();
    }

    /**
     * Test data health check passes when all data sources are fresh.
     */
    @Test
    public void testDataHealthCheck_AllSourcesFresh_ReturnsHealthy() {
        // Arrange - Create active data sources with fresh data
        createDataSource("MarketData", "API", "IEX Cloud", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        createDataSource("FundamentalData", "API", "Alpha Vantage", 24,
                LocalDateTime.now().minusHours(2), "HEALTHY");

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert
        assertThat(result.healthy()).isTrue();
        assertThat(result.summary()).contains("All 2 data sources are healthy");
        assertThat(result.healthySources()).hasSize(2);
        assertThat(result.staleSources()).isEmpty();
    }

    /**
     * Test data health check detects stale data sources.
     */
    @Test
    public void testDataHealthCheck_StaleSources_ReturnsUnhealthy() {
        // Arrange - Create mix of fresh and stale data sources
        createDataSource("MarketData", "API", "IEX Cloud", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        createDataSource("StaleData", "API", "Alpha Vantage", 24,
                LocalDateTime.now().minusHours(48), "STALE");

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert
        assertThat(result.healthy()).isFalse();
        assertThat(result.summary()).contains("1 of 2 data sources have stale data");
        assertThat(result.healthySources()).hasSize(1);
        assertThat(result.staleSources()).hasSize(1);
        assertThat(result.staleSources().get(0)).contains("StaleData");
        assertThat(result.staleSources().get(0)).contains("48 hours old");
    }

    /**
     * Test data health check detects data sources that never updated successfully.
     */
    @Test
    public void testDataHealthCheck_NeverUpdated_ReturnsUnhealthy() {
        // Arrange - Create data source that never successfully updated
        createDataSource("NewDataSource", "API", "New Provider", 24,
                null, "UNKNOWN");

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert
        assertThat(result.healthy()).isFalse();
        assertThat(result.summary()).contains("stale data");
        assertThat(result.staleSources()).hasSize(1);
        assertThat(result.staleSources().get(0)).contains("Never successfully fetched");
    }

    /**
     * Test data health check with different freshness thresholds.
     */
    @Test
    public void testDataHealthCheck_DifferentThresholds_AppliesCorrectly() {
        // Arrange - Create data source updated 12 hours ago
        createDataSource("MarketData", "API", "IEX Cloud", 24,
                LocalDateTime.now().minusHours(12), "HEALTHY");

        // Act & Assert - With 6-hour threshold, data is stale
        DataHealthResult result6h = dataSourceHealthService.checkDataHealth(6);
        assertThat(result6h.healthy()).isFalse();
        assertThat(result6h.staleSources()).hasSize(1);

        // Act & Assert - With 24-hour threshold, data is fresh
        DataHealthResult result24h = dataSourceHealthService.checkDataHealth(24);
        assertThat(result24h.healthy()).isTrue();
        assertThat(result24h.healthySources()).hasSize(1);
    }

    /**
     * Test data health check ignores inactive data sources.
     */
    @Test
    public void testDataHealthCheck_InactiveSources_AreIgnored() {
        // Arrange - Create active and inactive data sources
        DataSource activeSource = createDataSource("ActiveData", "API", "Provider A", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");

        DataSource inactiveSource = createDataSource("InactiveData", "API", "Provider B", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        inactiveSource.setIsActive(false);
        dataSourceRepository.save(inactiveSource);

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert - Only active source is checked
        assertThat(result.healthy()).isTrue();
        assertThat(result.healthySources()).hasSize(1);
        assertThat(result.healthySources().get(0)).contains("ActiveData");
    }

    /**
     * Test specific data source health check.
     */
    @Test
    public void testIsDataSourceHealthy_FreshSource_ReturnsTrue() {
        // Arrange
        createDataSource("TestSource", "API", "Test Provider", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");

        // Act
        boolean isHealthy = dataSourceHealthService.isDataSourceHealthy("TestSource");

        // Assert
        assertThat(isHealthy).isTrue();
    }

    /**
     * Test specific data source health check for stale source.
     */
    @Test
    public void testIsDataSourceHealthy_StaleSource_ReturnsFalse() {
        // Arrange
        createDataSource("StaleSource", "API", "Test Provider", 24,
                LocalDateTime.now().minusHours(48), "STALE");

        // Act
        boolean isHealthy = dataSourceHealthService.isDataSourceHealthy("StaleSource");

        // Assert
        assertThat(isHealthy).isFalse();
    }

    /**
     * Test specific data source health check for inactive source.
     */
    @Test
    public void testIsDataSourceHealthy_InactiveSource_ReturnsFalse() {
        // Arrange
        DataSource source = createDataSource("InactiveSource", "API", "Test Provider", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        source.setIsActive(false);
        dataSourceRepository.save(source);

        // Act
        boolean isHealthy = dataSourceHealthService.isDataSourceHealthy("InactiveSource");

        // Assert
        assertThat(isHealthy).isFalse();
    }

    /**
     * Test specific data source health check for non-existent source.
     */
    @Test
    public void testIsDataSourceHealthy_NonExistentSource_ReturnsFalse() {
        // Act
        boolean isHealthy = dataSourceHealthService.isDataSourceHealthy("NonExistent");

        // Assert
        assertThat(isHealthy).isFalse();
    }

    /**
     * Test data health check with no active data sources.
     */
    @Test
    public void testDataHealthCheck_NoActiveSources_ReturnsUnhealthy() {
        // Arrange - Create only inactive data sources
        DataSource source = createDataSource("InactiveData", "API", "Provider", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        source.setIsActive(false);
        dataSourceRepository.save(source);

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert
        assertThat(result.healthy()).isFalse();
        assertThat(result.summary()).contains("No active market data sources");
    }

    /**
     * Test data health check ignores non-API/FEED sources.
     */
    @Test
    public void testDataHealthCheck_OnlyApiAndFeedSources_AreChecked() {
        // Arrange - Create different source types
        createDataSource("API_Source", "API", "Provider A", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        createDataSource("FEED_Source", "FEED", "Provider B", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");
        createDataSource("MANUAL_Source", "MANUAL", "Provider C", 24,
                LocalDateTime.now().minusHours(1), "HEALTHY");

        // Act
        DataHealthResult result = dataSourceHealthService.checkDataHealth(24);

        // Assert - Only API and FEED sources are checked
        assertThat(result.healthy()).isTrue();
        assertThat(result.healthySources()).hasSize(2);
    }

    /**
     * Helper method to create a data source.
     */
    private DataSource createDataSource(String name, String sourceType, String provider,
                                       int stalenessThresholdHours,
                                       LocalDateTime lastSuccessfulUpdate,
                                       String healthStatus) {
        DataSource source = DataSource.builder()
                .name(name)
                .sourceType(sourceType)
                .provider(provider)
                .description("Test data source: " + name)
                .refreshFrequencyHours(24)
                .stalenessThresholdHours(stalenessThresholdHours)
                .lastSuccessfulUpdate(lastSuccessfulUpdate)
                .lastAttemptedUpdate(lastSuccessfulUpdate)
                .recordCount(1000L)
                .healthStatus(healthStatus)
                .consecutiveFailures(0)
                .uptimePct30d(BigDecimal.valueOf(99.5))
                .isCritical(true)
                .isActive(true)
                .build();

        return dataSourceRepository.save(source);
    }
}
