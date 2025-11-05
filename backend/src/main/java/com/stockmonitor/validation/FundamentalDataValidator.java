package com.stockmonitor.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fundamental data validator (T217). */
@Component
@Slf4j
public class FundamentalDataValidator {
  public boolean validate(String symbol, Object fundamentals) {
    // TODO: Implement validation
    return true;
  }
}
