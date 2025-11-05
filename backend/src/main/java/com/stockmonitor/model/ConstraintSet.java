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
@Table(name = "constraint_set")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintSet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String name;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = false;

  @Builder.Default
  @Column(name = "max_name_weight_large_cap_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal maxNameWeightLargeCapPct = new BigDecimal("5.00");

  @Builder.Default
  @Column(name = "max_name_weight_mid_cap_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal maxNameWeightMidCapPct = new BigDecimal("2.00");

  @Builder.Default
  @Column(name = "max_name_weight_small_cap_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal maxNameWeightSmallCapPct = new BigDecimal("1.00");

  @Builder.Default
  @Column(name = "max_sector_exposure_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal maxSectorExposurePct = new BigDecimal("20.00");

  @Builder.Default
  @Column(name = "turnover_cap_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal turnoverCapPct = new BigDecimal("25.00");

  @Builder.Default
  @Column(name = "weight_deadband_bps", nullable = false)
  private Integer weightDeadbandBps = 30;

  @Builder.Default
  @Column(name = "participation_cap_tier1_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal participationCapTier1Pct = new BigDecimal("10.00");

  @Builder.Default
  @Column(name = "participation_cap_tier2_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal participationCapTier2Pct = new BigDecimal("7.50");

  @Builder.Default
  @Column(name = "participation_cap_tier3_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal participationCapTier3Pct = new BigDecimal("5.00");

  @Builder.Default
  @Column(name = "participation_cap_tier4_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal participationCapTier4Pct = new BigDecimal("3.00");

  @Builder.Default
  @Column(name = "participation_cap_tier5_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal participationCapTier5Pct = new BigDecimal("1.00");

  @Builder.Default
  @Column(name = "spread_threshold_bps", nullable = false)
  private Integer spreadThresholdBps = 50;

  @Builder.Default
  @Column(name = "earnings_blackout_hours", nullable = false)
  private Integer earningsBlackoutHours = 48;

  @Builder.Default
  @Column(name = "liquidity_floor_adv_usd", precision = 19, scale = 4, nullable = false)
  private BigDecimal liquidityFloorAdvUsd = new BigDecimal("1000000.00");

  @Builder.Default
  @Column(name = "cost_margin_required_bps", nullable = false)
  private Integer costMarginRequiredBps = 20;

  @Builder.Default
  @Column(nullable = false)
  private Integer version = 1;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
