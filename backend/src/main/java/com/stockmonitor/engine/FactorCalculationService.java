package com.stockmonitor.engine;

import com.stockmonitor.dto.FactorScoreDTO;
import com.stockmonitor.model.Holding;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating factor scores for individual holdings.
 *
 * <p>Calculates raw factor scores (Value, Momentum, Quality, Revisions) before sector normalization.
 */
@Service("holdingFactorScoreCalculator")
@Slf4j
public class FactorCalculationService {

  /**
   * Calculate raw factor scores for a holding.
   *
   * @param holding The holding to calculate scores for
   * @return FactorScoreDTO with raw scores
   */
  public FactorScoreDTO calculateFactorScores(Holding holding) {
    // TODO: Implement actual factor calculations based on fundamental data
    // For now, return stub data

    return FactorScoreDTO.builder()
        .symbol(holding.getSymbol())
        .sector(holding.getSector())
        .value(BigDecimal.valueOf(Math.random() * 2 - 1)) // Random between -1 and 1
        .momentum(BigDecimal.valueOf(Math.random() * 2 - 1))
        .quality(BigDecimal.valueOf(Math.random() * 2 - 1))
        .revisions(BigDecimal.valueOf(Math.random() * 2 - 1))
        .composite(BigDecimal.ZERO) // Will be calculated after normalization
        .calculatedAt(LocalDateTime.now())
        .build();
  }
}
