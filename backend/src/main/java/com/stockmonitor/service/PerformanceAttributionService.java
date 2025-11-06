package com.stockmonitor.service;

import com.stockmonitor.dto.PerformanceMetricsDTO;
import com.stockmonitor.dto.PerformanceMetricsDTO.PerformanceContributorDTO;
import com.stockmonitor.model.Holding;
import com.stockmonitor.repository.HoldingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for performance attribution and P&L analysis (T157, FR-008, FR-014).
 *
 * <p>Calculates: - Total P&L (realized + unrealized) - Benchmark comparison - Top
 * contributors/detractors
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceAttributionService {

  private final HoldingRepository holdingRepository;

  /**
   * Calculate performance metrics for portfolio (FR-008, FR-014).
   *
   * @param portfolioId Portfolio UUID
   * @param startDate Period start date (null = last month)
   * @param endDate Period end date (null = today)
   * @return Performance metrics with contributors/detractors
   */
  @Transactional(readOnly = true)
  public PerformanceMetricsDTO calculatePerformance(
      UUID portfolioId, LocalDate startDate, LocalDate endDate) {
    log.info(
        "Calculating performance for portfolio {} from {} to {}", portfolioId, startDate,
         endDate);

    // Default to last month if dates not provided
    final LocalDate effectiveEndDate = (endDate == null) ? LocalDate.now() : endDate;
    final LocalDate effectiveStartDate = (startDate == null) ? effectiveEndDate.minusMonths(1) : startDate;

    List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

    if (holdings.isEmpty()) {
      log.warn("No holdings found for portfolio: {}", portfolioId);
      return buildEmptyPerformanceMetrics(effectiveStartDate, effectiveEndDate);
    }

    // Calculate P&L for each holding
    List<PerformanceContributorDTO> contributors =
        holdings.stream()
            .map(holding -> calculateHoldingPnL(holding, effectiveStartDate, effectiveEndDate))
            .sorted(Comparator.comparing(PerformanceContributorDTO::getPnl).reversed())
            .collect(Collectors.toList());

    // Calculate totals
    BigDecimal totalPnL =
        contributors.stream()
            .map(PerformanceContributorDTO::getPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal startingValue = calculatePortfolioValue(holdings, effectiveStartDate);
    BigDecimal endingValue = calculatePortfolioValue(holdings, effectiveEndDate);

    BigDecimal totalPnLPct =
        startingValue.compareTo(BigDecimal.ZERO) > 0
            ? totalPnL
                .divide(startingValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    // Get benchmark return (stubbed for now)
    BigDecimal benchmarkReturn = getBenchmarkReturn(effectiveStartDate, effectiveEndDate);
    BigDecimal excessReturn =
        benchmarkReturn != null ? totalPnLPct.subtract(benchmarkReturn) : null;

    // Split into contributors and detractors
    List<PerformanceContributorDTO> topContributors =
        contributors.stream()
            .filter(c -> c.getPnl().compareTo(BigDecimal.ZERO) > 0)
            .limit(5)
            .collect(Collectors.toList());

    List<PerformanceContributorDTO> topDetractors =
        contributors.stream()
            .filter(c -> c.getPnl().compareTo(BigDecimal.ZERO) < 0)
            .sorted(Comparator.comparing(PerformanceContributorDTO::getPnl))
            .limit(5)
            .collect(Collectors.toList());

    return PerformanceMetricsDTO.builder()
        .totalPnL(totalPnL)
        .totalPnLPct(totalPnLPct)
        .benchmarkReturn(benchmarkReturn)
        .excessReturn(excessReturn)
        .topContributors(topContributors)
        .topDetractors(topDetractors)
        .periodStart(effectiveStartDate)
        .periodEnd(effectiveEndDate)
        .startingValue(startingValue)
        .endingValue(endingValue)
        .tradeCount(0) // TODO: Calculate from trade history
        .transactionCosts(BigDecimal.ZERO) // TODO: Calculate from trade history
        .build();
  }

  /**
   * Calculate P&L contribution for a single holding (FR-014).
   *
   * @param holding Holding to analyze
   * @param startDate Period start
   * @param endDate Period end
   * @return Performance contribution DTO
   */
  private PerformanceContributorDTO calculateHoldingPnL(
      Holding holding, LocalDate startDate, LocalDate endDate) {
    // Get prices at start and end of period
    BigDecimal startPrice = getHistoricalPrice(holding.getSymbol(), startDate);
    BigDecimal endPrice = getHistoricalPrice(holding.getSymbol(), endDate);

    // Calculate P&L: (endPrice - startPrice) * quantity
    BigDecimal priceChange = endPrice.subtract(startPrice);
    BigDecimal pnl = priceChange.multiply(holding.getQuantity());

    // Calculate P&L percentage: ((endPrice - costBasis) / costBasis) * 100
    BigDecimal pnlPct =
        holding.getCostBasis().compareTo(BigDecimal.ZERO) > 0
            ? endPrice
                .subtract(holding.getCostBasis())
                .divide(holding.getCostBasis(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    // Calculate position weight (current market value / total portfolio value)
    BigDecimal marketValue = endPrice.multiply(holding.getQuantity());
    BigDecimal weight =
        BigDecimal.valueOf(5.0); // TODO: Calculate actual weight based on portfolio total

    return PerformanceContributorDTO.builder()
        .symbol(holding.getSymbol())
        .pnl(pnl)
        .pnlPct(pnlPct)
        .weight(weight)
        .sector(holding.getSector())
        .shares(holding.getQuantity())
        .costBasis(holding.getCostBasis())
        .currentPrice(endPrice)
        .build();
  }

  /**
   * Calculate total portfolio value at a specific date.
   *
   * @param holdings Portfolio holdings
   * @param date Valuation date
   * @return Total portfolio value
   */
  private BigDecimal calculatePortfolioValue(List<Holding> holdings, LocalDate date) {
    return holdings.stream()
        .map(
            h -> {
              BigDecimal price = getHistoricalPrice(h.getSymbol(), date);
              return price.multiply(h.getQuantity());
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Get historical price for a symbol (stubbed).
   *
   * <p>TODO: Integrate with market data service.
   *
   * @param symbol Stock symbol
   * @param date Date
   * @return Price at date
   */
  private BigDecimal getHistoricalPrice(String symbol, LocalDate date) {
    // Stub: Return mock price
    // In real implementation, fetch from market data service
    return BigDecimal.valueOf(150.00);
  }

  /**
   * Get benchmark return for period (stubbed).
   *
   * <p>TODO: Integrate with benchmark data service (S&P 500).
   *
   * @param startDate Period start
   * @param endDate Period end
   * @return Benchmark return percentage
   */
  private BigDecimal getBenchmarkReturn(LocalDate startDate, LocalDate endDate) {
    // Stub: Return mock benchmark return
    // In real implementation, fetch S&P 500 return from data service
    return BigDecimal.valueOf(3.0); // 3% return
  }

  /**
   * Build empty performance metrics for portfolio with no holdings.
   *
   * @param startDate Period start
   * @param endDate Period end
   * @return Empty performance metrics
   */
  private PerformanceMetricsDTO buildEmptyPerformanceMetrics(
      LocalDate startDate, LocalDate endDate) {
    return PerformanceMetricsDTO.builder()
        .totalPnL(BigDecimal.ZERO)
        .totalPnLPct(BigDecimal.ZERO)
        .benchmarkReturn(null)
        .excessReturn(null)
        .topContributors(Collections.emptyList())
        .topDetractors(Collections.emptyList())
        .periodStart(startDate)
        .periodEnd(endDate)
        .startingValue(BigDecimal.ZERO)
        .endingValue(BigDecimal.ZERO)
        .tradeCount(0)
        .transactionCosts(BigDecimal.ZERO)
        .build();
  }
}
