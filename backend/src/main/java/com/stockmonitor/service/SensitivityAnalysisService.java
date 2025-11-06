package com.stockmonitor.service;

import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.dto.SensitivityPreviewDTO;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sensitivity analysis (T181, FR-054, FR-055).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensitivityAnalysisService {

  /**
   * Analyze sensitivity of portfolio to constraint change.
   */
  public SensitivityPreviewDTO analyzeSensitivity(
      UUID portfolioId, String constraintName, BigDecimal newValue, ConstraintSetDTO current) {
    log.info("Analyzing sensitivity for {} = {}", constraintName, newValue);

    // TODO: Implement full sensitivity analysis
    // Stub returns mock preview
    return SensitivityPreviewDTO.builder()
        .constraintName(constraintName)
        .originalValue(BigDecimal.valueOf(10.0))
        .newValue(newValue)
        .expectedHoldingsDelta(2)
        .expectedTurnoverDelta(BigDecimal.valueOf(5.0))
        .expectedSectorConcentrationDelta(BigDecimal.valueOf(-2.0))
        .expectedReturnDelta(BigDecimal.valueOf(0.5))
        .expectedRiskDelta(BigDecimal.valueOf(0.2))
        .sensitivityScore(7)
        .impactSummary("Moderate impact on portfolio composition")
        .recommendation("Consider applying this change")
        .build();
  }
}
