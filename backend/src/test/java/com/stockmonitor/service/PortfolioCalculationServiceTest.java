package com.stockmonitor.service;

import com.stockmonitor.BaseUnitTest;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit test for portfolio calculation service.
 *
 * Tests:
 * - Total value calculation (cash + market value)
 * - Unrealized P&L calculation
 * - Weight percentage calculation
 * - Market value summation
 * - Edge cases (empty portfolio, zero quantities, negative P&L)
 */
public class PortfolioCalculationServiceTest extends BaseUnitTest {

    @InjectMocks
    private PortfolioCalculationService calculationService;

    private Portfolio portfolio;
    private List<Holding> holdings;

    @BeforeEach
    public void setup() {
        calculationService = new PortfolioCalculationService();

        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .cashBalance(BigDecimal.valueOf(10000))
                .build();

        holdings = new ArrayList<>();
    }

    @Test
    public void testCalculateTotalValue() {
        // Add holdings
        Holding aapl = createHolding("AAPL", 100, 150.0, 180.0);
        Holding msft = createHolding("MSFT", 50, 250.0, 300.0);

        holdings.add(aapl);
        holdings.add(msft);

        // AAPL: 100 * 180 = 18,000
        // MSFT: 50 * 300 = 15,000
        // Market Value: 33,000
        // Cash: 10,000
        // Total: 43,000

        BigDecimal marketValue = calculationService.calculateMarketValue(holdings);
        BigDecimal totalValue = calculationService.calculateTotalValue(portfolio, holdings);

        assertThat(marketValue).isEqualByComparingTo(new BigDecimal("33000"));
        assertThat(totalValue).isEqualByComparingTo(new BigDecimal("43000"));
    }

    @Test
    public void testCalculateUnrealizedPnl() {
        // AAPL: cost=15,000 (100*150), market=18,000 (100*180), unrealized=+3,000
        Holding aapl = createHolding("AAPL", 100, 150.0, 180.0);

        // MSFT: cost=12,500 (50*250), market=15,000 (50*300), unrealized=+2,500
        Holding msft = createHolding("MSFT", 50, 250.0, 300.0);

        holdings.add(aapl);
        holdings.add(msft);

        BigDecimal unrealizedPnl = calculationService.calculateUnrealizedPnL(holdings);

        // Total unrealized P&L = 3,000 + 2,500 = 5,500
        assertThat(unrealizedPnl).isEqualByComparingTo(new BigDecimal("5500"));
    }

    @Test
    public void testCalculateNegativeUnrealizedPnl() {
        // AAPL: cost=18,000 (100*180), market=15,000 (100*150), unrealized=-3,000
        Holding aapl = createHolding("AAPL", 100, 180.0, 150.0);

        holdings.add(aapl);

        BigDecimal unrealizedPnl = calculationService.calculateUnrealizedPnL(holdings);

        assertThat(unrealizedPnl).isEqualByComparingTo(new BigDecimal("-3000"));
    }

    @Test
    public void testCalculateWeightPercentages() {
        Holding aapl = createHolding("AAPL", 100, 150.0, 180.0); // Market value: 18,000
        Holding msft = createHolding("MSFT", 50, 250.0, 300.0);  // Market value: 15,000

        holdings.add(aapl);
        holdings.add(msft);

        BigDecimal totalValue = calculationService.calculateTotalValue(portfolio, holdings);

        // Total portfolio value: 43,000 (33,000 market + 10,000 cash)
        // AAPL weight: 18,000 / 43,000 = 41.86%
        // MSFT weight: 15,000 / 43,000 = 34.88%

        BigDecimal aaplWeight = calculationService.calculateWeight(aapl, totalValue);
        BigDecimal msftWeight = calculationService.calculateWeight(msft, totalValue);

        assertThat(aaplWeight.doubleValue()).isCloseTo(41.86, within(0.01));
        assertThat(msftWeight.doubleValue()).isCloseTo(34.88, within(0.01));
    }

    @Test
    public void testCalculateEmptyPortfolio() {
        // Portfolio with only cash, no holdings
        BigDecimal marketValue = calculationService.calculateMarketValue(holdings);
        BigDecimal totalValue = calculationService.calculateTotalValue(portfolio, holdings);
        BigDecimal unrealizedPnl = calculationService.calculateUnrealizedPnL(holdings);

        assertThat(marketValue).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totalValue).isEqualByComparingTo(new BigDecimal("10000")); // Just cash
        assertThat(unrealizedPnl).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void testCalculateZeroQuantity() {
        Holding zeroQuantity = createHolding("AAPL", 0, 150.0, 180.0);
        holdings.add(zeroQuantity);

        BigDecimal marketValue = calculationService.calculateMarketValue(holdings);

        assertThat(marketValue).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void testCalculateSingleHolding() {
        Holding aapl = createHolding("AAPL", 100, 150.0, 180.0);
        holdings.add(aapl);

        BigDecimal totalValue = calculationService.calculateTotalValue(portfolio, holdings);
        BigDecimal weight = calculationService.calculateWeight(aapl, totalValue);

        // Weight based on total portfolio value (including cash)
        // Market value: 18,000, Total value: 28,000 (18,000 + 10,000 cash)
        // Weight: 18,000 / 28,000 = 64.29%
        assertThat(weight.doubleValue()).isCloseTo(64.29, within(0.01));
    }

    @Test
    public void testCalculateFractionalQuantity() {
        // Some stocks allow fractional shares
        Holding brk = createHolding("BRK.B", 10.5, 350.25, 375.50);

        holdings.add(brk);

        BigDecimal marketValue = calculationService.calculateMarketValue(holdings);

        // Market value: 10.5 * 375.50 = 3,942.75
        BigDecimal expectedMarketValue = new BigDecimal("10.5").multiply(new BigDecimal("375.50"));
        assertThat(marketValue).isEqualByComparingTo(expectedMarketValue);
    }

    @Test
    public void testCalculateMultipleCurrencies() {
        // This test assumes FX conversion is handled elsewhere
        // Calculator should work with pre-converted USD values

        Holding usdStock = createHolding("AAPL", 100, 150.0, 180.0); // 18,000 USD
        Holding convertedGbpStock = createHolding("VOD.L", 1000, 1.50, 1.75); // Already converted to USD

        holdings.add(usdStock);
        holdings.add(convertedGbpStock);

        BigDecimal marketValue = calculationService.calculateMarketValue(holdings);

        BigDecimal expectedMarketValue = new BigDecimal("18000").add(new BigDecimal("1750"));
        assertThat(marketValue).isEqualByComparingTo(expectedMarketValue);
    }

    // Helper method to create holdings
    private Holding createHolding(String symbol, double quantity, double costBasisPerShare, double currentPrice) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal costBasis = BigDecimal.valueOf(costBasisPerShare);
        BigDecimal price = BigDecimal.valueOf(currentPrice);

        return Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolio.getId())
                .symbol(symbol)
                .quantity(qty)
                .costBasis(costBasis)
                .currentPrice(price)
                .build();
    }
}
