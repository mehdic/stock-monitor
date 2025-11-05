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

@Entity
@Table(name = "recommendation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "run_id", nullable = false)
  private UUID runId;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String symbol;

  @NotNull
  @Column(nullable = false)
  private Integer rank;

  @NotNull
  @Column(name = "target_weight_pct", nullable = false, precision = 5, scale = 2)
  private BigDecimal targetWeightPct;

  @Builder.Default
  @Column(name = "current_weight_pct", nullable = false, precision = 5, scale = 2)
  private BigDecimal currentWeightPct = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "weight_change_pct", nullable = false, precision = 5, scale = 2)
  private BigDecimal weightChangePct = BigDecimal.ZERO;

  @NotNull
  @Column(name = "confidence_score", nullable = false)
  private Integer confidenceScore;

  @NotNull
  @Column(name = "expected_cost_bps", nullable = false, precision = 7, scale = 2)
  private BigDecimal expectedCostBps;

  @NotNull
  @Column(name = "expected_alpha_bps", nullable = false, precision = 7, scale = 2)
  private BigDecimal expectedAlphaBps;

  @NotNull
  @Column(name = "edge_over_cost_bps", nullable = false, precision = 7, scale = 2)
  private BigDecimal edgeOverCostBps;

  @NotBlank
  @Column(name = "driver1_name", nullable = false, length = 50)
  private String driver1Name;

  @NotNull
  @Column(name = "driver1_score", nullable = false, precision = 7, scale = 4)
  private BigDecimal driver1Score;

  @NotBlank
  @Column(name = "driver2_name", nullable = false, length = 50)
  private String driver2Name;

  @NotNull
  @Column(name = "driver2_score", nullable = false, precision = 7, scale = 4)
  private BigDecimal driver2Score;

  @NotBlank
  @Column(name = "driver3_name", nullable = false, length = 50)
  private String driver3Name;

  @NotNull
  @Column(name = "driver3_score", nullable = false, precision = 7, scale = 4)
  private BigDecimal driver3Score;

  @NotBlank
  @Column(nullable = false, length = 1000)
  private String explanation;

  @Column(name = "constraint_notes", length = 500)
  private String constraintNotes;

  @Column(name = "risk_contribution_pct", precision = 5, scale = 2)
  private BigDecimal riskContributionPct;

  @Builder.Default
  @Column(name = "change_indicator", nullable = false, length = 20)
  private String changeIndicator = "NEW";

  @NotBlank
  @Column(nullable = false, length = 50)
  private String sector;

  @NotBlank
  @Column(name = "market_cap_tier", nullable = false, length = 20)
  private String marketCapTier;

  @NotNull
  @Column(name = "liquidity_tier", nullable = false)
  private Integer liquidityTier;

  @NotNull
  @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal currentPrice;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
