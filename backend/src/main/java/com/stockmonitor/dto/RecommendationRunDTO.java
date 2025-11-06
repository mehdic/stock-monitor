package com.stockmonitor.dto;

import com.stockmonitor.model.RecommendationRun;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRunDTO {
  private UUID id;
  private UUID userId;
  private UUID universeId;
  private UUID constraintSetId;
  private UUID portfolioId;
  private String runType;
  private String status;
  private LocalDate scheduledDate;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private Long executionDurationMs;
  private Integer recommendationCount;
  private Integer exclusionCount;
  private BigDecimal expectedTurnoverPct;
  private BigDecimal estimatedCostBps;
  private BigDecimal expectedAlphaBps;
  private String decision;
  private String decisionReason;
  private UUID previousRunId;
  private String errorMessage;
  private Boolean dataFreshnessCheckPassed;
  private Boolean constraintFeasibilityCheckPassed;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static RecommendationRunDTO from(RecommendationRun run) {
    return RecommendationRunDTO.builder()
        .id(run.getId())
        .userId(run.getUserId())
        .universeId(run.getUniverseId())
        .constraintSetId(run.getConstraintSetId())
        .portfolioId(run.getPortfolioId())
        .runType(run.getRunType())
        .status(run.getStatus())
        .scheduledDate(run.getScheduledDate())
        .startedAt(run.getStartedAt())
        .completedAt(run.getCompletedAt())
        .executionDurationMs(run.getExecutionDurationMs())
        .recommendationCount(run.getRecommendationCount())
        .exclusionCount(run.getExclusionCount())
        .expectedTurnoverPct(run.getExpectedTurnoverPct())
        .estimatedCostBps(run.getEstimatedCostBps())
        .expectedAlphaBps(run.getExpectedAlphaBps())
        .decision(run.getDecision())
        .decisionReason(run.getDecisionReason())
        .previousRunId(run.getPreviousRunId())
        .errorMessage(run.getErrorMessage())
        .dataFreshnessCheckPassed(run.getDataFreshnessCheckPassed())
        .constraintFeasibilityCheckPassed(run.getConstraintFeasibilityCheckPassed())
        .createdAt(run.getCreatedAt())
        .updatedAt(run.getUpdatedAt())
        .build();
  }
}
