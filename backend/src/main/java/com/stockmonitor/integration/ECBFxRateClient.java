package com.stockmonitor.integration;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** ECB FX rate client (T207). */
@Component
@Slf4j
public class ECBFxRateClient {
  public BigDecimal getExchangeRate(String from, String to) {
    log.debug("Fetching FX rate: {} to {}", from, to);
    return BigDecimal.ONE;
  }
}
