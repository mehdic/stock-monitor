package com.stockmonitor.engine;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for calculating transaction costs and edge-over-cost (T198, FR-026, FR-069).
 */
@Component
@Slf4j
public class CostCalculationService {

  /**
   * Calculate total transaction cost for trade.
   */
  public BigDecimal calculateTransactionCost(
      String symbol, BigDecimal shares, BigDecimal price) {
    // Commission + spread + market impact
    BigDecimal commission = BigDecimal.valueOf(0.001); // 0.1%
    BigDecimal spread = BigDecimal.valueOf(0.002); // 0.2%
    BigDecimal marketImpact = BigDecimal.valueOf(0.001); // 0.1%

    BigDecimal totalCostPct = commission.add(spread).add(marketImpact);
    return price.multiply(shares).multiply(totalCostPct);
  }

  /**
   * Calculate edge-over-cost with safe margin (T198, FR-069).
   *
   * <p>Edge = Expected alpha from factor scores Cost = Transaction costs Safe margin = 1.5x
   * (require 50% buffer)
   *
   * @param expectedAlpha Expected return advantage (percentage)
   * @param transactionCost Transaction cost (percentage)
   * @return true if edge > cost * 1.5
   */
  public boolean hasEdgeOverCost(BigDecimal expectedAlpha, BigDecimal transactionCost) {
    BigDecimal safeMargin = BigDecimal.valueOf(1.5);
    BigDecimal costWithMargin = transactionCost.multiply(safeMargin);

    boolean hasEdge = expectedAlpha.compareTo(costWithMargin) > 0;

    log.debug(
        "Edge-over-cost check: alpha={}, cost={}, margin={}, hasEdge={}",
        expectedAlpha,
        transactionCost,
        costWithMargin,
        hasEdge);

    return hasEdge;
  }

  /**
   * Calculate expected alpha from factor scores (stub).
   */
  public BigDecimal calculateExpectedAlpha(BigDecimal compositeScore) {
    // Stub: Convert z-score to expected alpha percentage
    // z-score of 1.0 ≈ 5% expected alpha
    return compositeScore.multiply(BigDecimal.valueOf(5.0));
  }
}
