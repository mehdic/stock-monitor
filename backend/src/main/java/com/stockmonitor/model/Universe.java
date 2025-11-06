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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "universe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Universe {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String type;

  @NotBlank
  @Column(name = "benchmark_symbol", nullable = false, length = 20)
  private String benchmarkSymbol;

  @NotNull
  @Column(name = "constituent_count", nullable = false)
  private Integer constituentCount;

  @Column(name = "min_market_cap", precision = 19, scale = 4)
  private BigDecimal minMarketCap;

  @Column(name = "max_market_cap", precision = 19, scale = 4)
  private BigDecimal maxMarketCap;

  @Builder.Default
  @Type(JsonType.class)
  @Column(name = "liquidity_tier_threshold", nullable = false, columnDefinition = "TEXT")
  private String liquidityTierThreshold = "{}";

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Builder.Default
  @Column(nullable = false)
  private Integer version = 1;

  @NotNull
  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
