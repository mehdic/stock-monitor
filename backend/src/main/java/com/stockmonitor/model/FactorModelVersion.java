package com.stockmonitor.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "factor_model_version")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorModelVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(name = "version_number", nullable = false, unique = true, length = 20)
  private String versionNumber;

  @NotNull
  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = false;

  @Type(JsonType.class)
  @Column(name = "value_definition", columnDefinition = "jsonb", nullable = false)
  private String valueDefinition = "{}";

  @Type(JsonType.class)
  @Column(name = "momentum_definition", columnDefinition = "jsonb", nullable = false)
  private String momentumDefinition = "{}";

  @Type(JsonType.class)
  @Column(name = "quality_definition", columnDefinition = "jsonb", nullable = false)
  private String qualityDefinition = "{}";

  @Type(JsonType.class)
  @Column(name = "revisions_definition", columnDefinition = "jsonb", nullable = false)
  private String revisionsDefinition = "{}";

  @NotBlank
  @Column(name = "sector_neutralization_method", nullable = false, length = 100)
  private String sectorNeutralizationMethod;

  @Column(name = "winsorization_percentile", precision = 5, scale = 2, nullable = false)
  private BigDecimal winsorizationPercentile = new BigDecimal("1.00");

  @Type(JsonType.class)
  @Column(name = "composite_weighting", columnDefinition = "jsonb", nullable = false)
  private String compositeWeighting = "{}";

  @Column(length = 1000)
  private String description;

  @NotBlank
  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "approved_by", length = 100)
  private String approvedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
