package com.stockmonitor.dto;

import com.stockmonitor.model.Recommendation;
import java.math.BigDecimal;
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
public class RecommendationDTO {
  private UUID id;
  private UUID runId;
  private String symbol;
  private Integer rank;
  private BigDecimal targetWeightPct;
  private BigDecimal currentWeightPct;
  private BigDecimal weightChangePct;
  private Integer confidenceScore;
  private BigDecimal expectedCostBps;
  private BigDecimal expectedAlphaBps;
  private BigDecimal edgeOverCostBps;
  private String driver1Name;
  private BigDecimal driver1Score;
  private String driver2Name;
  private BigDecimal driver2Score;
  private String driver3Name;
  private BigDecimal driver3Score;
  private String explanation;
  private String constraintNotes;
  private BigDecimal riskContributionPct;
  private String changeIndicator;
  private String sector;
  private String marketCapTier;
  private Integer liquidityTier;
  private BigDecimal currentPrice;
  private LocalDateTime createdAt;

  public static RecommendationDTO from(Recommendation recommendation) {
    return RecommendationDTO.builder()
        .id(recommendation.getId())
        .runId(recommendation.getRunId())
        .symbol(recommendation.getSymbol())
        .rank(recommendation.getRank())
        .targetWeightPct(recommendation.getTargetWeightPct())
        .currentWeightPct(recommendation.getCurrentWeightPct())
        .weightChangePct(recommendation.getWeightChangePct())
        .confidenceScore(recommendation.getConfidenceScore())
        .expectedCostBps(recommendation.getExpectedCostBps())
        .expectedAlphaBps(recommendation.getExpectedAlphaBps())
        .edgeOverCostBps(recommendation.getEdgeOverCostBps())
        .driver1Name(recommendation.getDriver1Name())
        .driver1Score(recommendation.getDriver1Score())
        .driver2Name(recommendation.getDriver2Name())
        .driver2Score(recommendation.getDriver2Score())
        .driver3Name(recommendation.getDriver3Name())
        .driver3Score(recommendation.getDriver3Score())
        .explanation(recommendation.getExplanation())
        .constraintNotes(recommendation.getConstraintNotes())
        .riskContributionPct(recommendation.getRiskContributionPct())
        .changeIndicator(recommendation.getChangeIndicator())
        .sector(recommendation.getSector())
        .marketCapTier(recommendation.getMarketCapTier())
        .liquidityTier(recommendation.getLiquidityTier())
        .currentPrice(recommendation.getCurrentPrice())
        .createdAt(recommendation.getCreatedAt())
        .build();
  }
}
