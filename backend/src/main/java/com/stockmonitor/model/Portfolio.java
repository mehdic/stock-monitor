package com.stockmonitor.model;

import jakarta.persistence.*;
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
@Table(name = "portfolio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Builder.Default
  @Column(name = "cash_balance", precision = 19, scale = 4, nullable = false)
  private BigDecimal cashBalance = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "total_market_value", precision = 19, scale = 4, nullable = false)
  private BigDecimal totalMarketValue = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "total_cost_basis", precision = 19, scale = 4, nullable = false)
  private BigDecimal totalCostBasis = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "unrealized_pnl", precision = 19, scale = 4, nullable = false)
  private BigDecimal unrealizedPnl = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "unrealized_pnl_pct", precision = 10, scale = 6, nullable = false)
  private BigDecimal unrealizedPnlPct = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "benchmark_return_pct", precision = 10, scale = 6, nullable = false)
  private BigDecimal benchmarkReturnPct = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "relative_return_pct", precision = 10, scale = 6, nullable = false)
  private BigDecimal relativeReturnPct = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "universe_coverage_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal universeCoveragePct = BigDecimal.ZERO;

  @Column(name = "active_universe_id")
  private UUID activeUniverseId;

  @Column(name = "active_constraint_set_id")
  private UUID activeConstraintSetId;

  @Builder.Default
  @Column(name = "last_calculated_at", nullable = false)
  private LocalDateTime lastCalculatedAt = LocalDateTime.now();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
