package com.stockmonitor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for data source health status (T159, FR-037, FR-038).
 *
 * <p>Tracks freshness and availability of external data sources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceHealthDTO {

  /** Data source identifier (e.g., "market-data", "factor-data"). */
  private String id;

  /** Human-readable name. */
  private String name;

  /** Health status: HEALTHY, STALE, UNAVAILABLE. */
  private HealthStatus status;

  /** Timestamp of last successful data update. */
  private LocalDateTime lastUpdateTime;

  /** Configured freshness threshold in minutes. */
  private Integer freshnessThresholdMinutes;

  /** Minutes since last update. */
  private Long minutesSinceUpdate;

  /** Human-readable status message. */
  private String message;

  /** Number of consecutive failures (if UNAVAILABLE). */
  private Integer consecutiveFailures;

  /** Next scheduled check time. */
  private LocalDateTime nextCheckTime;

  /** Data source type (API, DATABASE, FILE_FEED). */
  private String sourceType;

  /**
   * Health status enum (FR-037).
   *
   * <p>HEALTHY: Data is fresh (within threshold) STALE: Data exceeds freshness threshold
   * UNAVAILABLE: Data source unreachable or no data
   */
  public enum HealthStatus {
    HEALTHY,
    STALE,
    UNAVAILABLE
  }

  /**
   * Check if data source is healthy.
   *
   * @return true if status is HEALTHY
   */
  public boolean isHealthy() {
    return status == HealthStatus.HEALTHY;
  }

  /**
   * Check if data source blocks recommendation runs (FR-037).
   *
   * <p>Critical sources (market-data, factor-data) block runs if UNAVAILABLE. STALE sources
   * generate warnings but don't block.
   *
   * @return true if this health status should block recommendation runs
   */
  public boolean blocksRecommendationRuns() {
    // UNAVAILABLE status blocks runs for critical sources
    if (status == HealthStatus.UNAVAILABLE) {
      return isCriticalSource();
    }
    return false;
  }

  /**
   * Check if this is a critical data source.
   *
   * @return true if this source is critical for recommendation runs
   */
  private boolean isCriticalSource() {
    return id.equals("market-data") || id.equals("factor-data");
  }

  /**
   * Create health DTO from last update time and threshold.
   *
   * @param id Data source ID
   * @param name Data source name
   * @param lastUpdate Last update timestamp
   * @param thresholdMinutes Freshness threshold
   * @return DataSourceHealthDTO with calculated status
   */
  public static DataSourceHealthDTO from(
      String id, String name, LocalDateTime lastUpdate, int thresholdMinutes) {
    if (lastUpdate == null) {
      return DataSourceHealthDTO.builder()
          .id(id)
          .name(name)
          .status(HealthStatus.UNAVAILABLE)
          .freshnessThresholdMinutes(thresholdMinutes)
          .message("No data available")
          .build();
    }

    long minutesSince =
        java.time.Duration.between(lastUpdate, LocalDateTime.now()).toMinutes();
    HealthStatus status =
        minutesSince <= thresholdMinutes ? HealthStatus.HEALTHY : HealthStatus.STALE;

    String message =
        status == HealthStatus.HEALTHY
            ? String.format("Data is fresh (updated %d minutes ago)", minutesSince)
            : String.format(
                "Data is stale (updated %d minutes ago, threshold: %d minutes)",
                minutesSince, thresholdMinutes);

    return DataSourceHealthDTO.builder()
        .id(id)
        .name(name)
        .status(status)
        .lastUpdateTime(lastUpdate)
        .freshnessThresholdMinutes(thresholdMinutes)
        .minutesSinceUpdate(minutesSince)
        .message(message)
        .build();
  }
}
