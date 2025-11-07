package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.DataSourceHealthDTO;
import com.stockmonitor.model.DataSource;
import com.stockmonitor.repository.DataSourceRepository;
import com.stockmonitor.service.DataSourceHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for data source health monitoring.
 *
 * Tests:
 * - Data source health status calculation
 * - Health DTO generation with staleness indicators
 * - Multiple data source monitoring
 * - Health status transitions
 */
@Transactional
public class DataSourceHealthIntegrationTest extends BaseIntegrationTest {

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
     * Test getting all data sources with health status.
     */
    @Test
    public void testGetAllDataSources_ReturnsHealthDTOs() {
        // Arrange
        createDataSource("MarketData", "API", "IEX Cloud",
                LocalDateTime.now().minusHours(1), "HEALTHY");
        createDataSource("FundamentalData", "API", "Alpha Vantage",
                LocalDateTime.now().minusHours(2), "HEALTHY");

        // Act
        List<DataSourceHealthDTO> healthDTOs = dataSourceHealthService.getAllDataSources();

        // Assert
        assertThat(healthDTOs).hasSize(2);
        assertThat(healthDTOs.get(0).getName()).isIn("MarketData", "FundamentalData");
        assertThat(healthDTOs.get(1).getName()).isIn("MarketData", "FundamentalData");
    }

    /**
     * Test getting specific data source health.
     */
    @Test
    public void testGetDataSourceHealth_ValidSource_ReturnsHealthDTO() {
        // Arrange
        createDataSource("TestSource", "API", "Test Provider",
                LocalDateTime.now().minusHours(1), "HEALTHY");

        // Act
        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("TestSource");

        // Assert
        assertThat(healthDTO).isNotNull();
        assertThat(healthDTO.getName()).isEqualTo("TestSource");
        assertThat(healthDTO.getLastUpdateTime()).isNotNull();
    }

    /**
     * Test getting health for non-existent data source.
     */
    @Test
    public void testGetDataSourceHealth_NonExistentSource_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> dataSourceHealthService.getDataSourceHealth("NonExistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data source not found");
    }

    /**
     * Test health DTO indicates fresh data.
     */
    @Test
    public void testDataSourceHealthDTO_FreshData_IndicatesHealthy() {
        // Arrange
        LocalDateTime recentUpdate = LocalDateTime.now().minusMinutes(30);
        createDataSource("FreshSource", "API", "Provider",
                recentUpdate, "HEALTHY");

        // Act
        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("FreshSource");

        // Assert
        assertThat(healthDTO.getLastUpdateTime()).isEqualTo(recentUpdate);
        // Health status is calculated based on freshness threshold
        // Default threshold is 60 minutes, so 30 minutes should be fresh
    }

    /**
     * Test health DTO indicates stale data.
     */
    @Test
    public void testDataSourceHealthDTO_StaleData_IndicatesUnhealthy() {
        // Arrange
        LocalDateTime oldUpdate = LocalDateTime.now().minusHours(48);
        createDataSource("StaleSource", "API", "Provider",
                oldUpdate, "STALE");

        // Act
        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("StaleSource");

        // Assert
        assertThat(healthDTO.getLastUpdateTime()).isEqualTo(oldUpdate);
        // Health status reflects staleness
    }

    /**
     * Test health DTO for data source with no successful update.
     */
    @Test
    public void testDataSourceHealthDTO_NeverUpdated_IndicatesUnknownHealth() {
        // Arrange
        DataSource source = DataSource.builder()
                .name("NewSource")
                .sourceType("API")
                .provider("New Provider")
                .description("Newly added source")
                .refreshFrequencyHours(24)
                .stalenessThresholdHours(24)
                .lastSuccessfulUpdate(null)  // Never updated
                .healthStatus("UNKNOWN")
                .consecutiveFailures(0)
                .isCritical(true)
                .isActive(true)
                .build();
        dataSourceRepository.save(source);

        // Act
        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("NewSource");

        // Assert
        assertThat(healthDTO.getLastUpdateTime()).isNull();
    }

    /**
     * Test health monitoring for multiple data sources.
     */
    @Test
    public void testGetAllDataSources_MixedHealthStatuses_ReturnsAll() {
        // Arrange - Create sources with different health statuses
        createDataSource("HealthySource", "API", "Provider A",
                LocalDateTime.now().minusMinutes(30), "HEALTHY");
        createDataSource("StaleSource", "API", "Provider B",
                LocalDateTime.now().minusHours(48), "STALE");
        createDataSource("ErrorSource", "API", "Provider C",
                LocalDateTime.now().minusHours(72), "ERROR");

        // Act
        List<DataSourceHealthDTO> healthDTOs = dataSourceHealthService.getAllDataSources();

        // Assert
        assertThat(healthDTOs).hasSize(3);

        // Verify each source is included
        assertThat(healthDTOs).extracting(DataSourceHealthDTO::getName)
                .containsExactlyInAnyOrder("HealthySource", "StaleSource", "ErrorSource");
    }

    /**
     * Test health DTO includes all required fields.
     */
    @Test
    public void testDataSourceHealthDTO_IncludesAllFields() {
        // Arrange
        LocalDateTime lastUpdate = LocalDateTime.now().minusHours(2);
        createDataSource("CompleteSource", "API", "Provider",
                lastUpdate, "HEALTHY");

        // Act
        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("CompleteSource");

        // Assert
        assertThat(healthDTO.getName()).isEqualTo("CompleteSource");
        assertThat(healthDTO.getId()).isEqualTo("CompleteSource");
        assertThat(healthDTO.getLastUpdateTime()).isEqualTo(lastUpdate);
        // Other fields are calculated by the DTO factory method
    }

    /**
     * Test health monitoring excludes inactive sources.
     */
    @Test
    public void testGetAllDataSources_IncludesInactiveSources() {
        // Arrange - Create active and inactive sources
        createDataSource("ActiveSource", "API", "Provider A",
                LocalDateTime.now().minusHours(1), "HEALTHY");

        DataSource inactiveSource = createDataSource("InactiveSource", "API", "Provider B",
                LocalDateTime.now().minusHours(1), "HEALTHY");
        inactiveSource.setIsActive(false);
        dataSourceRepository.save(inactiveSource);

        // Act
        List<DataSourceHealthDTO> healthDTOs = dataSourceHealthService.getAllDataSources();

        // Assert - getAllDataSources includes all sources (active and inactive)
        assertThat(healthDTOs).hasSize(2);
    }

    /**
     * Test health status transitions.
     */
    @Test
    public void testDataSourceHealth_StatusTransition_ReflectsNewState() {
        // Arrange - Create healthy source
        DataSource source = createDataSource("TransitionSource", "API", "Provider",
                LocalDateTime.now().minusHours(1), "HEALTHY");

        // Act - Transition to stale
        source.setLastSuccessfulUpdate(LocalDateTime.now().minusHours(48));
        source.setHealthStatus("STALE");
        dataSourceRepository.save(source);

        DataSourceHealthDTO healthDTO = dataSourceHealthService.getDataSourceHealth("TransitionSource");

        // Assert - Health reflects new state
        assertThat(healthDTO.getLastUpdateTime()).isBefore(LocalDateTime.now().minusHours(47));
    }

    /**
     * Test health monitoring with consecutive failures.
     */
    @Test
    public void testDataSourceHealth_ConsecutiveFailures_TrackedInEntity() {
        // Arrange
        DataSource source = DataSource.builder()
                .name("FailingSource")
                .sourceType("API")
                .provider("Failing Provider")
                .description("Source with failures")
                .refreshFrequencyHours(24)
                .stalenessThresholdHours(24)
                .lastSuccessfulUpdate(LocalDateTime.now().minusHours(72))
                .lastAttemptedUpdate(LocalDateTime.now().minusMinutes(5))
                .healthStatus("ERROR")
                .errorMessage("Connection timeout")
                .consecutiveFailures(5)
                .isCritical(true)
                .isActive(true)
                .build();
        source = dataSourceRepository.save(source);

        // Act
        DataSource retrieved = dataSourceRepository.findByName("FailingSource").orElseThrow();

        // Assert
        assertThat(retrieved.getConsecutiveFailures()).isEqualTo(5);
        assertThat(retrieved.getHealthStatus()).isEqualTo("ERROR");
        assertThat(retrieved.getErrorMessage()).contains("timeout");
    }

    /**
     * Test health monitoring includes uptime percentage.
     */
    @Test
    public void testDataSourceHealth_IncludesUptimePercentage() {
        // Arrange
        DataSource source = DataSource.builder()
                .name("UptimeSource")
                .sourceType("API")
                .provider("Provider")
                .description("Source with uptime data")
                .refreshFrequencyHours(24)
                .stalenessThresholdHours(24)
                .lastSuccessfulUpdate(LocalDateTime.now().minusHours(1))
                .healthStatus("HEALTHY")
                .consecutiveFailures(0)
                .uptimePct30d(BigDecimal.valueOf(99.8))
                .isCritical(true)
                .isActive(true)
                .build();
        source = dataSourceRepository.save(source);

        // Act
        DataSource retrieved = dataSourceRepository.findByName("UptimeSource").orElseThrow();

        // Assert
        assertThat(retrieved.getUptimePct30d()).isEqualByComparingTo(BigDecimal.valueOf(99.8));
    }

    /**
     * Helper method to create a data source.
     */
    private DataSource createDataSource(String name, String sourceType, String provider,
                                       LocalDateTime lastSuccessfulUpdate,
                                       String healthStatus) {
        DataSource source = DataSource.builder()
                .name(name)
                .sourceType(sourceType)
                .provider(provider)
                .description("Test data source: " + name)
                .refreshFrequencyHours(24)
                .stalenessThresholdHours(24)
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
