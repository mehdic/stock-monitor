package com.stockmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "data_source")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @NotBlank
  @Column(name = "source_type", nullable = false, length = 20)
  private String sourceType;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String provider;

  @Column(length = 500)
  private String description;

  @NotNull
  @Column(name = "refresh_frequency_hours", nullable = false)
  private Integer refreshFrequencyHours;

  @NotNull
  @Column(name = "staleness_threshold_hours", nullable = false)
  private Integer stalenessThresholdHours;

  @Column(name = "last_successful_update")
  private LocalDateTime lastSuccessfulUpdate;

  @Column(name = "last_attempted_update")
  private LocalDateTime lastAttemptedUpdate;

  @Column(name = "last_update_duration_ms")
  private Long lastUpdateDurationMs;

  @Column(name = "record_count")
  private Long recordCount;

  @Column(name = "health_status", length = 20, nullable = false)
  private String healthStatus = "UNKNOWN";

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "consecutive_failures", nullable = false)
  private Integer consecutiveFailures = 0;

  @Column(name = "uptime_pct_30d", precision = 5, scale = 2)
  private BigDecimal uptimePct30d;

  @Column(name = "is_critical", nullable = false)
  private Boolean isCritical = true;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
