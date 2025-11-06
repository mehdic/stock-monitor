package com.stockmonitor.dto;

import com.stockmonitor.model.ConstraintSet;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ConstraintSetDTO {
  private UUID id;
  private UUID userId;
  private String name;
  private Boolean isActive;

  @DecimalMin(value = "0.0", message = "Max name weight for large cap must be at least 0%")
  @DecimalMax(value = "100.0", message = "Max name weight for large cap cannot exceed 100%")
  private BigDecimal maxNameWeightLargeCapPct;

  @DecimalMin(value = "0.0", message = "Max name weight for mid cap must be at least 0%")
  @DecimalMax(value = "100.0", message = "Max name weight for mid cap cannot exceed 100%")
  private BigDecimal maxNameWeightMidCapPct;

  @DecimalMin(value = "0.0", message = "Max name weight for small cap must be at least 0%")
  @DecimalMax(value = "100.0", message = "Max name weight for small cap cannot exceed 100%")
  private BigDecimal maxNameWeightSmallCapPct;

  @DecimalMin(value = "0.0", message = "Max sector exposure must be at least 0%")
  @DecimalMax(value = "100.0", message = "Max sector exposure cannot exceed 100%")
  private BigDecimal maxSectorExposurePct;

  @DecimalMin(value = "0.0", message = "Turnover cap must be at least 0%")
  @DecimalMax(value = "100.0", message = "Turnover cap cannot exceed 100%")
  private BigDecimal turnoverCapPct;
  private Integer weightDeadbandBps;
  private BigDecimal participationCapTier1Pct;
  private BigDecimal participationCapTier2Pct;
  private BigDecimal participationCapTier3Pct;
  private BigDecimal participationCapTier4Pct;
  private BigDecimal participationCapTier5Pct;
  private Integer spreadThresholdBps;
  private Integer earningsBlackoutHours;
  private BigDecimal liquidityFloorAdvUsd;
  private Integer costMarginRequiredBps;
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ConstraintSetDTO from(ConstraintSet constraintSet) {
    return ConstraintSetDTO.builder()
        .id(constraintSet.getId())
        .userId(constraintSet.getUserId())
        .name(constraintSet.getName())
        .isActive(constraintSet.getIsActive())
        .maxNameWeightLargeCapPct(constraintSet.getMaxNameWeightLargeCapPct())
        .maxNameWeightMidCapPct(constraintSet.getMaxNameWeightMidCapPct())
        .maxNameWeightSmallCapPct(constraintSet.getMaxNameWeightSmallCapPct())
        .maxSectorExposurePct(constraintSet.getMaxSectorExposurePct())
        .turnoverCapPct(constraintSet.getTurnoverCapPct())
        .weightDeadbandBps(constraintSet.getWeightDeadbandBps())
        .participationCapTier1Pct(constraintSet.getParticipationCapTier1Pct())
        .participationCapTier2Pct(constraintSet.getParticipationCapTier2Pct())
        .participationCapTier3Pct(constraintSet.getParticipationCapTier3Pct())
        .participationCapTier4Pct(constraintSet.getParticipationCapTier4Pct())
        .participationCapTier5Pct(constraintSet.getParticipationCapTier5Pct())
        .spreadThresholdBps(constraintSet.getSpreadThresholdBps())
        .earningsBlackoutHours(constraintSet.getEarningsBlackoutHours())
        .liquidityFloorAdvUsd(constraintSet.getLiquidityFloorAdvUsd())
        .costMarginRequiredBps(constraintSet.getCostMarginRequiredBps())
        .version(constraintSet.getVersion())
        .createdAt(constraintSet.getCreatedAt())
        .updatedAt(constraintSet.getUpdatedAt())
        .build();
  }

  public static ConstraintSetDTO getDefaults() {
    return ConstraintSetDTO.builder()
        .name("Default Constraints")
        .isActive(false)
        .maxNameWeightLargeCapPct(new BigDecimal("10.00"))
        .maxNameWeightMidCapPct(new BigDecimal("2.00"))
        .maxNameWeightSmallCapPct(new BigDecimal("1.00"))
        .maxSectorExposurePct(new BigDecimal("30.00"))
        .turnoverCapPct(new BigDecimal("25.00"))
        .weightDeadbandBps(30)
        .participationCapTier1Pct(new BigDecimal("10.00"))
        .participationCapTier2Pct(new BigDecimal("7.50"))
        .participationCapTier3Pct(new BigDecimal("5.00"))
        .participationCapTier4Pct(new BigDecimal("3.00"))
        .participationCapTier5Pct(new BigDecimal("1.00"))
        .spreadThresholdBps(50)
        .earningsBlackoutHours(48)
        .liquidityFloorAdvUsd(new BigDecimal("1000000.00"))
        .costMarginRequiredBps(20)
        .version(1)
        .build();
  }
}
