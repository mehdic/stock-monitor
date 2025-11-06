package com.stockmonitor.validation;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Price anomaly detector (T218). */
@Component
@Slf4j
public class PriceAnomalyDetector {
  public boolean detectAnomaly(String symbol, BigDecimal price, BigDecimal previousPrice) {
    if (previousPrice == null) return false;

    BigDecimal change = price.subtract(previousPrice).divide(previousPrice, 4, java.math.RoundingMode.HALF_UP);
    if (change.abs().compareTo(BigDecimal.valueOf(0.5)) > 0) {
      log.warn("Price anomaly detected for {}: {}% change", symbol, change.multiply(BigDecimal.valueOf(100)));
      return true;
    }
    return false;
  }
}
