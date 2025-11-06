package com.stockmonitor.service;

import com.stockmonitor.dto.DataSourceHealthDTO;
import com.stockmonitor.model.DataSource;
import com.stockmonitor.repository.DataSourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checking health and freshness of data sources.
 *
 * <p>Ensures that data used for recommendations is fresh and reliable before running calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceHealthService {

  private final DataSourceRepository dataSourceRepository;

  /**
   * Check if all data sources are healthy and data is fresh.
   *
   * @param freshnessThresholdHours Maximum age of data in hours
   * @return DataHealthResult with overall status and details
   */
  @Transactional(readOnly = true)
  public DataHealthResult checkDataHealth(int freshnessThresholdHours) {
    log.info("Checking data source health with {} hour freshness threshold", freshnessThresholdHours);

    List<DataSource> allSources = dataSourceRepository.findAll();
    List<DataSource> activeSources =
        allSources.stream()
            .filter(DataSource::getIsActive)
            .filter(ds -> ds.getSourceType().equals("API") || ds.getSourceType().equals("FEED"))
            .toList();

    if (activeSources.isEmpty()) {
      log.warn("No active market data sources found");
      return new DataHealthResult(
          false, "No active market data sources configured", List.of(), List.of());
    }

    LocalDateTime freshnessThreshold = LocalDateTime.now().minusHours(freshnessThresholdHours);
    List<String> staleDataSources = new ArrayList<>();
    List<String> healthyDataSources = new ArrayList<>();

    for (DataSource source : activeSources) {
      LocalDateTime lastSuccess = source.getLastSuccessfulUpdate();

      if (lastSuccess == null) {
        staleDataSources.add(
            String.format("%s: Never successfully fetched", source.getName()));
      } else if (lastSuccess.isBefore(freshnessThreshold)) {
        long hoursStale =
            java.time.Duration.between(lastSuccess, LocalDateTime.now()).toHours();
        staleDataSources.add(
            String.format("%s: Data %d hours old", source.getName(), hoursStale));
      } else {
        long hoursOld =
            java.time.Duration.between(lastSuccess, LocalDateTime.now()).toHours();
        healthyDataSources.add(
            String.format("%s: Fresh (%d hours old)", source.getName(), hoursOld));
      }
    }

    boolean allHealthy = staleDataSources.isEmpty();
    String summary =
        allHealthy
            ? String.format("All %d data sources are healthy", healthyDataSources.size())
            : String.format(
                "%d of %d data sources have stale data",
                staleDataSources.size(), activeSources.size());

    log.info("Data health check complete: {}", summary);

    return new DataHealthResult(allHealthy, summary, healthyDataSources, staleDataSources);
  }

  /**
   * Check if a specific data source is healthy.
   *
   * @param sourceName Name of the data source
   * @return true if source is active and fresh
   */
  @Transactional(readOnly = true)
  public boolean isDataSourceHealthy(String sourceName) {
    return dataSourceRepository
        .findByName(sourceName)
        .map(
            source -> {
              if (!source.getIsActive()) {
                return false;
              }

              LocalDateTime lastSuccess = source.getLastSuccessfulUpdate();
              if (lastSuccess == null) {
                return false;
              }

              LocalDateTime threshold = LocalDateTime.now().minusHours(24);
              return lastSuccess.isAfter(threshold);
            })
        .orElse(false);
  }

  /**
   * Get all data sources with health status (T161, FR-037, FR-038).
   *
   * @return List of data source health DTOs
   */
  @Transactional(readOnly = true)
  public List<DataSourceHealthDTO> getAllDataSources() {
    log.info("Getting health status for all data sources");

    List<DataSource> allSources = dataSourceRepository.findAll();

    return allSources.stream()
        .map(this::mapToHealthDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get detailed health information for specific data source (T161, FR-037, FR-038).
   *
   * @param id Data source ID
   * @return Data source health DTO
   */
  @Transactional(readOnly = true)
  public DataSourceHealthDTO getDataSourceHealth(String id) {
    log.info("Getting health for data source: {}", id);

    DataSource source =
        dataSourceRepository
            .findByName(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Data source not found: " + id));

    return mapToHealthDTO(source);
  }

  /**
   * Map DataSource entity to DataSourceHealthDTO with calculated health status (T161).
   *
   * @param source Data source entity
   * @return DataSourceHealthDTO with health status
   */
  private DataSourceHealthDTO mapToHealthDTO(DataSource source) {
    // Get freshness threshold from source configuration (default 60 minutes)
    int thresholdMinutes = 60; // TODO: Get from source configuration

    // Use static factory method from DTO
    return DataSourceHealthDTO.from(
        source.getName(),
        source.getName(),
        source.getLastSuccessfulUpdate(),
        thresholdMinutes);
  }

  /** Result of data health check */
  public record DataHealthResult(
      boolean healthy, String summary, List<String> healthySources, List<String> staleSources) {}
}
