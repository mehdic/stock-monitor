package com.stockmonitor.validation;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Price data validator (T216). */
@Component
@Slf4j
public class PriceDataValidator {

  public boolean validate(String symbol, BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Invalid price for {}: {}", symbol, price);
      return false;
    }
    return true;
  }
}
