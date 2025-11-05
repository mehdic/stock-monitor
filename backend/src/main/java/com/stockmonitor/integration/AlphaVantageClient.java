package com.stockmonitor.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Alpha Vantage client for end-of-day prices (T205).
 */
@Component
@Slf4j
public class AlphaVantageClient {

  public BigDecimal getPrice(String symbol, LocalDate date) {
    // TODO: Integrate with Alpha Vantage API
    log.debug("Fetching price for {} on {}", symbol, date);
    return BigDecimal.valueOf(150.00);
  }
}
