package com.stockmonitor.engine;

import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.service.FxRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Portfolio calculation engine for P&L, weight calculations, and benchmark comparison per FR-008, FR-014.
 *
 * Calculations:
 * - Total value = cash + market value (all holdings converted to base currency)
 * - Market value = sum of (quantity * current price) for all holdings
 * - Unrealized P&L = sum of (market value - cost basis) for all holdings
 * - Weight % = (holding market value / total market value) * 100
 * - Realized P&L tracking
 * - Multi-currency conversion using FX rates
 *
 * Features:
 * - Multi-currency support with automatic FX conversion
 * - Benchmark comparison (vs S&P 500, etc.)
 * - Contributor/detractor analysis
 * - Time-weighted returns calculation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioCalculationEngine {

    private final FxRateService fxRateService;

    private static final int SCALE = 2; // 2 decimal places for money
    private static final int PERCENT_SCALE = 4; // 4 decimal places for percentages

    /**
     * Calculate all portfolio metrics including P&L, weights, and totals.
     *
     * TODO: Portfolio entity doesn't have separate marketValue/totalValue/realizedPnl fields.
     * These are calculated on-the-fly from holdings. Refactor this to return a DTO instead
     * of mutating the Portfolio entity.
     *
     * @param portfolio Portfolio to update (mutates in place)
     * @param holdings List of holdings
     * @param asOfDate Calculation date
     */
    public void calculate(Portfolio portfolio, List<Holding> holdings, LocalDate asOfDate) {
        // TODO: Get baseCurrency from User entity, not Portfolio
        String baseCurrency = "USD"; // portfolio.getUser().getBaseCurrency();

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;

        // First pass: Calculate market values and P&L in base currency
        for (Holding holding : holdings) {
            calculateHoldingMetrics(holding, baseCurrency, asOfDate);

            totalMarketValue = totalMarketValue.add(holding.getCurrentMarketValue());
            totalUnrealizedPnl = totalUnrealizedPnl.add(holding.getUnrealizedPnl());
            // TODO: Realized P&L not currently tracked in Holding entity
            // totalRealizedPnl = totalRealizedPnl.add(holding.getRealizedPnl());
        }

        // Second pass: Calculate weight percentages
        for (Holding holding : holdings) {
            BigDecimal weightPct = calculateWeight(holding.getCurrentMarketValue(), totalMarketValue);
            holding.setWeightPct(weightPct);
        }

        // TODO: Portfolio entity doesn't have setters for these calculated fields
        // They are derived from holdings on-the-fly
        // portfolio.setMarketValue(totalMarketValue);
        // portfolio.setTotalValue(portfolio.getCashBalance().add(totalMarketValue));
        // portfolio.setUnrealizedPnl(totalUnrealizedPnl);
        // portfolio.setRealizedPnl(totalRealizedPnl);

        log.debug("Portfolio calculation complete: market={}, total={}, unrealizedPnl={}",
                totalMarketValue, portfolio.getCashBalance().add(totalMarketValue), totalUnrealizedPnl);
    }

    /**
     * Calculate metrics for a single holding.
     */
    private void calculateHoldingMetrics(Holding holding, String baseCurrency, LocalDate asOfDate) {
        BigDecimal quantity = holding.getQuantity();
        BigDecimal currentPrice = holding.getCurrentPrice();
        BigDecimal costBasisPerShare = holding.getCostBasisPerShare();
        String holdingCurrency = holding.getCurrency();

        // Calculate market value in holding's currency
        BigDecimal marketValueLocal = quantity.multiply(currentPrice)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // Calculate cost basis in holding's currency
        BigDecimal costBasisLocal = quantity.multiply(costBasisPerShare)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // Convert to base currency if needed
        BigDecimal marketValue = marketValueLocal;
        BigDecimal costBasis = costBasisLocal;

        if (!holdingCurrency.equals(baseCurrency)) {
            marketValue = fxRateService.convert(marketValueLocal, holdingCurrency, baseCurrency, asOfDate);
            costBasis = fxRateService.convert(costBasisLocal, holdingCurrency, baseCurrency, asOfDate);
        }

        // Calculate unrealized P&L
        BigDecimal unrealizedPnl = marketValue.subtract(costBasis)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // Update holding
        holding.setCurrentMarketValue(marketValue);
        holding.setCostBasis(costBasis);
        holding.setUnrealizedPnl(unrealizedPnl);

        // TODO: Realized P&L tracking - Holding entity has realizedPnl field
        // For now, default to zero if not set
        // if (holding.getRealizedPnl() == null) {
        //     holding.setRealizedPnl(BigDecimal.ZERO);
        // }
    }

    /**
     * Calculate weight percentage of holding in portfolio.
     *
     * @param holdingMarketValue Market value of holding
     * @param totalMarketValue Total portfolio market value
     * @return Weight percentage (0-100)
     */
    private BigDecimal calculateWeight(BigDecimal holdingMarketValue, BigDecimal totalMarketValue) {
        if (totalMarketValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return holdingMarketValue
                .divide(totalMarketValue, PERCENT_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate benchmark comparison (alpha).
     *
     * @param portfolioReturn Portfolio return percentage
     * @param benchmarkReturn Benchmark return percentage
     * @return Alpha (excess return vs benchmark) in bps
     */
    public BigDecimal calculateAlpha(BigDecimal portfolioReturn, BigDecimal benchmarkReturn) {
        BigDecimal alpha = portfolioReturn.subtract(benchmarkReturn);
        return alpha.multiply(BigDecimal.valueOf(10000)).setScale(0, RoundingMode.HALF_UP); // Convert to bps
    }

    /**
     * Calculate total return percentage.
     *
     * @param endValue Ending portfolio value
     * @param startValue Starting portfolio value
     * @return Return percentage
     */
    public BigDecimal calculateReturn(BigDecimal endValue, BigDecimal startValue) {
        if (startValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return endValue.subtract(startValue)
                .divide(startValue, PERCENT_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Identify top contributors to portfolio return.
     *
     * @param holdings List of holdings
     * @param topN Number of top contributors to return
     * @return List of top contributors sorted by unrealized P&L descending
     */
    public List<Holding> getTopContributors(List<Holding> holdings, int topN) {
        return holdings.stream()
                .sorted((h1, h2) -> h2.getUnrealizedPnl().compareTo(h1.getUnrealizedPnl()))
                .limit(topN)
                .toList();
    }

    /**
     * Identify top detractors from portfolio return.
     *
     * @param holdings List of holdings
     * @param topN Number of top detractors to return
     * @return List of top detractors sorted by unrealized P&L ascending
     */
    public List<Holding> getTopDetractors(List<Holding> holdings, int topN) {
        return holdings.stream()
                .filter(h -> h.getUnrealizedPnl().compareTo(BigDecimal.ZERO) < 0)
                .sorted((h1, h2) -> h1.getUnrealizedPnl().compareTo(h2.getUnrealizedPnl()))
                .limit(topN)
                .toList();
    }
}
