package com.stockmonitor.model;

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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "universe_constituent")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniverseConstituent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "universe_id", nullable = false)
  private UUID universeId;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String symbol;

  @NotBlank
  @Column(name = "company_name", nullable = false)
  private String companyName;

  @NotBlank
  @Column(nullable = false, length = 50)
  private String sector;

  @Column(length = 100)
  private String industry;

  @NotBlank
  @Column(name = "market_cap_tier", nullable = false, length = 20)
  private String marketCapTier;

  @NotNull
  @Column(name = "liquidity_tier", nullable = false)
  private Integer liquidityTier;

  @NotNull
  @Column(name = "avg_daily_volume", nullable = false, precision = 19, scale = 2)
  private BigDecimal avgDailyVolume;

  @NotNull
  @Column(name = "avg_daily_value", nullable = false, precision = 19, scale = 4)
  private BigDecimal avgDailyValue;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @NotNull
  @Column(name = "added_date", nullable = false)
  private LocalDate addedDate;

  @Column(name = "removed_date")
  private LocalDate removedDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
