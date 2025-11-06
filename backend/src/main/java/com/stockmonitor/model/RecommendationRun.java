package com.stockmonitor.model;

import jakarta.persistence.*;
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
@Table(name = "recommendation_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRun {

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
  @Column(name = "portfolio_id", nullable = false)
  private UUID portfolioId;

  @NotNull
  @Column(name = "constraint_set_id", nullable = false)
  private UUID constraintSetId;

  @NotNull
  @Column(name = "run_type", nullable = false, length = 20)
  private String runType;

  @Builder.Default
  @Column(nullable = false, length = 20)
  private String status = "SCHEDULED";

  @Column(name = "scheduled_date")
  private LocalDate scheduledDate;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "execution_duration_ms")
  private Long executionDurationMs;

  @Builder.Default
  @Column(name = "recommendation_count", nullable = false)
  private Integer recommendationCount = 0;

  @Builder.Default
  @Column(name = "exclusion_count", nullable = false)
  private Integer exclusionCount = 0;

  @Column(name = "expected_turnover_pct", precision = 5, scale = 2)
  private BigDecimal expectedTurnoverPct;

  @Column(name = "estimated_cost_bps", precision = 7, scale = 2)
  private BigDecimal estimatedCostBps;

  @Column(name = "expected_alpha_bps", precision = 7, scale = 2)
  private BigDecimal expectedAlphaBps;

  @Builder.Default
  @Column(nullable = false, length = 20)
  private String decision = "PENDING";

  @Column(name = "decision_reason", length = 500)
  private String decisionReason;

  @Column(name = "previous_run_id")
  private UUID previousRunId;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Builder.Default
  @Column(name = "data_freshness_check_passed", nullable = false)
  private Boolean dataFreshnessCheckPassed = false;

  @Builder.Default
  @Column(name = "constraint_feasibility_check_passed", nullable = false)
  private Boolean constraintFeasibilityCheckPassed = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
