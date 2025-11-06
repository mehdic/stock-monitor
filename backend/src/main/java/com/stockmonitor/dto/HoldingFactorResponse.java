package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for holding factor analysis with nested structure.
 * Matches API contract expectations with separate factorScores, rawScores, and percentileRanks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingFactorResponse {

  private String symbol;
  private String sector;
  private LocalDateTime calculatedAt;
  private FactorScores factorScores;
  private RawScores rawScores;
  private PercentileRanks percentileRanks;

  /**
   * Nested factor scores (sector-normalized z-scores).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FactorScores {
    private BigDecimal value;
    private BigDecimal momentum;
    private BigDecimal quality;
    private BigDecimal revisions;
  }

  /**
   * Raw scores before sector normalization.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RawScores {
    private BigDecimal value;
    private BigDecimal momentum;
    private BigDecimal quality;
    private BigDecimal revisions;
  }

  /**
   * Percentile ranks within sector (0-100).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PercentileRanks {
    private Integer value;
    private Integer momentum;
    private Integer quality;
    private Integer revisions;
  }

  /**
   * Convert from FactorScoreDTO to HoldingFactorResponse.
   */
  public static HoldingFactorResponse from(FactorScoreDTO dto) {
    return HoldingFactorResponse.builder()
        .symbol(dto.getSymbol())
        .sector(dto.getSector())
        .calculatedAt(dto.getCalculatedAt())
        .factorScores(FactorScores.builder()
            .value(dto.getValue())
            .momentum(dto.getMomentum())
            .quality(dto.getQuality())
            .revisions(dto.getRevisions())
            .build())
        .rawScores(RawScores.builder()
            .value(dto.getRawValue())
            .momentum(dto.getRawMomentum())
            .quality(dto.getRawQuality())
            .revisions(dto.getRawRevisions())
            .build())
        .percentileRanks(PercentileRanks.builder()
            .value(dto.getValuePercentile())
            .momentum(dto.getMomentumPercentile())
            .quality(dto.getQualityPercentile())
            .revisions(dto.getRevisionsPercentile())
            .build())
        .build();
  }
}
