package com.stockmonitor.service;

import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for portfolio value calculations (FR-008).
 *
 * <p>Provides calculations for: - Total portfolio value - Market value of holdings - Unrealized
 * P&L - Position weights
 */
@Service
@Slf4j
public class PortfolioCalculationService {

  /**
   * Calculate total portfolio value (cash + market value of holdings).
   *
   * @param portfolio Portfolio
   * @param holdings Holdings list
   * @return Total portfolio value
   */
  public BigDecimal calculateTotalValue(Portfolio portfolio, List<Holding> holdings) {
    BigDecimal marketValue = calculateMarketValue(holdings);
    BigDecimal cash = portfolio.getCashBalance() != null ? portfolio.getCashBalance() : BigDecimal.ZERO;
    return cash.add(marketValue);
  }

  /**
   * Calculate market value of all holdings.
   *
   * @param holdings Holdings list
   * @return Total market value
   */
  public BigDecimal calculateMarketValue(List<Holding> holdings) {
    return holdings.stream()
        .map(
            h -> {
              BigDecimal price = h.getCurrentPrice() != null ? h.getCurrentPrice() : BigDecimal.ZERO;
              BigDecimal quantity = h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO;
              return price.multiply(quantity);
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Calculate total unrealized P&L.
   *
   * @param holdings Holdings list
   * @return Total unrealized P&L
   */
  public BigDecimal calculateUnrealizedPnL(List<Holding> holdings) {
    return holdings.stream()
        .map(
            h -> {
              BigDecimal currentPrice =
                  h.getCurrentPrice() != null ? h.getCurrentPrice() : BigDecimal.ZERO;
              BigDecimal costBasis = h.getCostBasis() != null ? h.getCostBasis() : BigDecimal.ZERO;
              BigDecimal quantity = h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO;
              return currentPrice.subtract(costBasis).multiply(quantity);
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Calculate weight percentage for a holding.
   *
   * @param holding Holding
   * @param totalPortfolioValue Total portfolio value
   * @return Weight percentage (0-100)
   */
  public BigDecimal calculateWeight(Holding holding, BigDecimal totalPortfolioValue) {
    if (totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal price = holding.getCurrentPrice() != null ? holding.getCurrentPrice() : BigDecimal.ZERO;
    BigDecimal quantity = holding.getQuantity() != null ? holding.getQuantity() : BigDecimal.ZERO;
    BigDecimal marketValue = price.multiply(quantity);

    return marketValue
        .divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }
}
