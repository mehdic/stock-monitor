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
@Table(name = "factor_score")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorScore {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String symbol;

  @NotBlank
  @Column(name = "factor_type", nullable = false, length = 20)
  private String factorType;

  @NotNull
  @Column(name = "calculation_date", nullable = false)
  private LocalDate calculationDate;

  @NotNull
  @Column(name = "raw_score", nullable = false, precision = 10, scale = 6)
  private BigDecimal rawScore;

  @NotNull
  @Column(name = "sector_normalized_score", nullable = false, precision = 10, scale = 6)
  private BigDecimal sectorNormalizedScore;

  @NotNull
  @Column(name = "percentile_rank_sector", nullable = false, precision = 5, scale = 2)
  private BigDecimal percentileRankSector;

  @NotNull
  @Column(name = "percentile_rank_universe", nullable = false, precision = 5, scale = 2)
  private BigDecimal percentileRankUniverse;

  @NotBlank
  @Column(nullable = false, length = 50)
  private String sector;

  @Type(JsonType.class)
  @Column(name = "component_breakdown", columnDefinition = "jsonb", nullable = false)
  private String componentBreakdown = "{}";

  @Column(name = "data_quality_score", nullable = false)
  private Integer dataQualityScore = 100;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
