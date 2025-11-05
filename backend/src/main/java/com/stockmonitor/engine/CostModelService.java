package com.stockmonitor.engine;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Cost model for backtesting (T179).
 */
@Component
@Slf4j
public class CostModelService {

  /**
   * Calculate transaction cost for trade.
   */
  public BigDecimal calculateTransactionCost(String symbol, BigDecimal shares, BigDecimal price) {
    // TODO: Implement realistic cost model (commission + spread + market impact)
    // Stub: 0.1% of trade value
    return price.multiply(shares).multiply(BigDecimal.valueOf(0.001));
  }
}
