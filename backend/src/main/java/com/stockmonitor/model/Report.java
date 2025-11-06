package com.stockmonitor.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "run_id", nullable = false, unique = true)
  private UUID runId;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotBlank
  @Column(name = "report_type", nullable = false, length = 20)
  private String reportType;

  @Column(nullable = false, length = 10)
  private String format = "HTML";

  @Column(name = "file_path", length = 500)
  private String filePath;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @NotBlank
  @Column(name = "summary_text", nullable = false, length = 2000)
  private String summaryText;

  @NotNull
  @Column(name = "recommendation_count", nullable = false)
  private Integer recommendationCount;

  @NotNull
  @Column(name = "exclusion_count", nullable = false)
  private Integer exclusionCount;

  @Type(JsonType.class)
  @Column(name = "constraint_snapshot", nullable = false, columnDefinition = "TEXT")
  private String constraintSnapshot = "{}";

  @Type(JsonType.class)
  @Column(name = "performance_metrics", columnDefinition = "TEXT")
  private String performanceMetrics;

  @Type(JsonType.class)
  @Column(name = "cost_analysis", columnDefinition = "TEXT")
  private String costAnalysis;

  @NotBlank
  @Column(name = "disclaimer_text", nullable = false, length = 5000)
  private String disclaimerText;

  @NotBlank
  @Column(name = "disclaimer_version", nullable = false, length = 20)
  private String disclaimerVersion;

  @Column(name = "generation_timestamp", nullable = false)
  private LocalDateTime generationTimestamp = LocalDateTime.now();

  @Column(name = "downloaded_at")
  private LocalDateTime downloadedAt;

  @Column(name = "download_count", nullable = false)
  private Integer downloadCount = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
