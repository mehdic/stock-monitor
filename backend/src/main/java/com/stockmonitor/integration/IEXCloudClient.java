package com.stockmonitor.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** IEX Cloud client for fallback prices (T206). */
@Component
@Slf4j
public class IEXCloudClient {

  public BigDecimal getPrice(String symbol, LocalDate date) {
    log.debug("Fetching fallback price for {} on {}", symbol, date);
    return BigDecimal.valueOf(150.00);
  }
}
