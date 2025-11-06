package com.stockmonitor.validation;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fundamental data anomaly detector (T218.5, FR-040). */
@Component
@Slf4j
public class FundamentalAnomalyDetector {

  public boolean detectAnomaly(String symbol, BigDecimal value, BigDecimal mean, BigDecimal stdDev) {
    if (stdDev.compareTo(BigDecimal.ZERO) == 0) return false;

    BigDecimal zScore = value.subtract(mean).divide(stdDev, 2, java.math.RoundingMode.HALF_UP);
    if (zScore.abs().compareTo(BigDecimal.valueOf(3.0)) > 0) {
      log.warn("Fundamental anomaly for {}: z-score = {}", symbol, zScore);
      return true;
    }
    return false;
  }
}
