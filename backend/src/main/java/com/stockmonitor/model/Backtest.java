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
@Table(name = "backtest")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Backtest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotNull
  @Column(name = "universe_id", nullable = false)
  private UUID universeId;

  @NotNull
  @Column(name = "constraint_set_id", nullable = false)
  private UUID constraintSetId;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String name;

  @NotNull
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @NotNull
  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "initial_capital", precision = 19, scale = 4, nullable = false)
  private BigDecimal initialCapital = new BigDecimal("1000000.00");

  @Column(name = "final_value", precision = 19, scale = 4)
  private BigDecimal finalValue;

  @Column(name = "total_return_pct", precision = 10, scale = 6)
  private BigDecimal totalReturnPct;

  @Column(name = "cagr_pct", precision = 10, scale = 6)
  private BigDecimal cagrPct;

  @Column(name = "volatility_pct", precision = 10, scale = 6)
  private BigDecimal volatilityPct;

  @Column(name = "sharpe_ratio", precision = 7, scale = 4)
  private BigDecimal sharpeRatio;

  @Column(name = "max_drawdown_pct", precision = 10, scale = 6)
  private BigDecimal maxDrawdownPct;

  @Column(name = "hit_rate_pct", precision = 5, scale = 2)
  private BigDecimal hitRatePct;

  @Column(name = "avg_turnover_pct", precision = 5, scale = 2)
  private BigDecimal avgTurnoverPct;

  @Column(name = "total_cost_bps", precision = 10, scale = 2)
  private BigDecimal totalCostBps;

  @Column(name = "benchmark_return_pct", precision = 10, scale = 6)
  private BigDecimal benchmarkReturnPct;

  @Column(name = "alpha_pct", precision = 10, scale = 6)
  private BigDecimal alphaPct;

  @Column(name = "beat_equal_weight")
  private Boolean beatEqualWeight;

  @Column(name = "verdict_text", length = 500)
  private String verdictText;

  @Type(JsonType.class)
  @Column(name = "equity_curve_data", nullable = false, columnDefinition = "TEXT")
  private String equityCurveData = "[]";

  @Type(JsonType.class)
  @Column(name = "turnover_history", nullable = false, columnDefinition = "TEXT")
  private String turnoverHistory = "[]";

  @Type(JsonType.class)
  @Column(name = "cost_assumptions", nullable = false, columnDefinition = "TEXT")
  private String costAssumptions = "{}";

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "execution_duration_ms")
  private Long executionDurationMs;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
