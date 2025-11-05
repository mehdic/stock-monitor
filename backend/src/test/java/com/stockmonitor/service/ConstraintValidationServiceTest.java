package com.stockmonitor.service;

import com.stockmonitor.BaseUnitTest;
import com.stockmonitor.engine.ConstraintEvaluationService;
import com.stockmonitor.model.ConstraintSet;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.UniverseConstituent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for constraint validation.
 *
 * Tests:
 * - Position size limits (large/mid/small cap)
 * - Sector exposure limits
 * - Turnover cap validation
 * - Liquidity tier checks
 * - Spread threshold validation
 * - Weight deadband logic
 */
public class ConstraintValidationServiceTest extends BaseUnitTest {

    @InjectMocks
    private ConstraintEvaluationService constraintService;

    private ConstraintSet constraints;
    private List<Holding> currentHoldings;
    private UUID universeId;

    @BeforeEach
    public void setup() {
        constraintService = new ConstraintEvaluationService();

        // Default constraints per spec
        constraints = ConstraintSet.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .maxNameWeightLargeCapPct(BigDecimal.valueOf(5.0))
                .maxNameWeightMidCapPct(BigDecimal.valueOf(2.0))
                .maxNameWeightSmallCapPct(BigDecimal.valueOf(1.0))
                .maxSectorExposurePct(BigDecimal.valueOf(25.0))
                .turnoverCapPct(BigDecimal.valueOf(25.0))
                .weightDeadbandBps(10) // Integer, not BigDecimal - 0.10%
                .participationCapTier1Pct(BigDecimal.valueOf(10.0))
                .participationCapTier2Pct(BigDecimal.valueOf(7.5))
                .participationCapTier3Pct(BigDecimal.valueOf(5.0))
                .participationCapTier4Pct(BigDecimal.valueOf(3.0))
                .participationCapTier5Pct(BigDecimal.valueOf(1.0))
                .spreadThresholdBps(50) // Integer, not BigDecimal - 0.50%
                .earningsBlackoutHours(72) // Integer, not BigDecimal
                .liquidityFloorAdvUsd(BigDecimal.valueOf(1000000))
                .costMarginRequiredBps(25) // Integer, not BigDecimal - 0.25%
                .isActive(true)
                .build();

        currentHoldings = new ArrayList<>();
        universeId = UUID.randomUUID();
    }

    @Test
    public void testValidLargeCapPosition() {
        UniverseConstituent constituent = createConstituent("AAPL", "Large", "Technology", 1);
        BigDecimal targetWeight = BigDecimal.valueOf(4.0); // 4% - within 5% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("AAPL", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    public void testExceedLargeCapPositionLimit() {
        UniverseConstituent constituent = createConstituent("AAPL", "Large", "Technology", 1);
        BigDecimal targetWeight = BigDecimal.valueOf(6.0); // 6% - exceeds 5% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("AAPL", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("Position size exceeds"));
        assertThat(result.violations()).anyMatch(v -> v.contains("5.0%"));
    }

    @Test
    public void testValidMidCapPosition() {
        UniverseConstituent constituent = createConstituent("MID", "Mid", "Healthcare", 2);
        BigDecimal targetWeight = BigDecimal.valueOf(1.8); // 1.8% - within 2% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("MID", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isTrue();
    }

    @Test
    public void testExceedMidCapPositionLimit() {
        UniverseConstituent constituent = createConstituent("MID", "Mid", "Healthcare", 2);
        BigDecimal targetWeight = BigDecimal.valueOf(2.5); // 2.5% - exceeds 2% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("MID", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("2.0%"));
    }

    @Test
    public void testValidSmallCapPosition() {
        UniverseConstituent constituent = createConstituent("SMALL", "Small", "Consumer", 3);
        BigDecimal targetWeight = BigDecimal.valueOf(0.8); // 0.8% - within 1% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("SMALL", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isTrue();
    }

    @Test
    public void testExceedSmallCapPositionLimit() {
        UniverseConstituent constituent = createConstituent("SMALL", "Small", "Consumer", 3);
        BigDecimal targetWeight = BigDecimal.valueOf(1.5); // 1.5% - exceeds 1% limit

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("SMALL", targetWeight, constituent, constraints, currentHoldings);

        assertThat(result.passed()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("1.0%"));
    }

    @Test
    public void testLiquidityTierRestriction() {
        // Tier 5 (lowest liquidity) - very restrictive
        UniverseConstituent lowLiquidity = createConstituent("ILLIQUID", "Large", "Technology", 5);
        BigDecimal targetWeight = BigDecimal.valueOf(2.0); // 2% - exceeds Tier 5 cap of 1%

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("ILLIQUID", targetWeight, lowLiquidity, constraints, currentHoldings);

        assertThat(result.passed()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("liquidity") || v.contains("participation"));
    }

    @Test
    public void testSectorExposureLimit() {
        // This test would require sector aggregation logic
        // For now, test that constraint checking includes sector validation

        UniverseConstituent tech1 = createConstituent("AAPL", "Large", "Technology", 1);
        BigDecimal weight1 = BigDecimal.valueOf(4.0);

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("AAPL", weight1, tech1, constraints, currentHoldings);

        // Individual position is valid
        assertThat(result.passed()).isTrue();

        // TODO: Test cumulative sector exposure when multiple holdings exist
    }

    @Test
    public void testWeightDeadband() {
        // If weight change is less than deadband, should be flagged as warning
        UniverseConstituent constituent = createConstituent("AAPL", "Large", "Technology", 1);

        // Create current holding at 4.00% of portfolio
        // Portfolio value needs to be $450,000 for AAPL to be 4%
        // AAPL market value = 100 * 180 = $18,000
        // Total portfolio = $18,000 / 0.04 = $450,000
        // Add another holding to make up the rest
        Holding currentHolding = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("AAPL")
                .quantity(BigDecimal.valueOf(100))
                .costBasis(BigDecimal.valueOf(150))
                .currentPrice(BigDecimal.valueOf(180)) // 18,000 market value
                .build();
        currentHoldings.add(currentHolding);

        // Add other holdings to make total portfolio ~$450,000
        Holding otherHolding = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("MSFT")
                .quantity(BigDecimal.valueOf(1000))
                .currentPrice(BigDecimal.valueOf(432)) // 432,000 market value (96%)
                .build();
        currentHoldings.add(otherHolding);

        // Target weight 4.08% - change of 0.08% = 8 bps (less than 10 bps deadband)
        BigDecimal targetWeight = BigDecimal.valueOf(4.08);

        ConstraintEvaluationService.ConstraintEvaluationResult result =
                constraintService.evaluateConstraints("AAPL", targetWeight, constituent, constraints, currentHoldings);

        // Should pass but include warning about deadband
        assertThat(result.warnings()).anyMatch(w -> w.contains("deadband") || w.contains("small change"));
    }

    @Test
    public void testTurnoverCalculation() {
        // Create current holdings with equal values (each 50% of portfolio)
        // Total portfolio value = 10,000 + 10,000 = 20,000
        Holding holding1 = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("AAPL")
                .quantity(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(100)) // 100 * 100 = 10,000 (50%)
                .build();

        Holding holding2 = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("MSFT")
                .quantity(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(100)) // 100 * 100 = 10,000 (50%)
                .build();

        currentHoldings.add(holding1);
        currentHoldings.add(holding2);

        // Current: AAPL 50%, MSFT 50%
        // Target: AAPL 50%, MSFT 30%
        // Changes: AAPL 0%, MSFT 20% reduction
        // Total absolute change = 0 + 20 = 20%
        // Turnover = 20% / 2 = 10%

        List<String> targetSymbols = List.of("AAPL", "MSFT");
        List<BigDecimal> targetWeights = List.of(
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(30.0)
        );

        BigDecimal turnover = constraintService.calculateTurnover(currentHoldings, targetSymbols, targetWeights);

        assertThat(turnover).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    }

    @Test
    public void testHighTurnoverViolation() {
        // Create scenario where turnover exceeds 25% cap

        for (int i = 0; i < 30; i++) {
            currentHoldings.add(Holding.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(UUID.randomUUID())
                    .symbol("STOCK" + i)
                    .quantity(BigDecimal.valueOf(100))
                    .build());
        }

        // Replace all holdings with new ones = 100% turnover
        // Turnover = 100% / 2 = 50% (exceeds 25% cap)

        List<String> newSymbols = new ArrayList<>();
        List<BigDecimal> newWeights = new ArrayList<>();
        for (int i = 30; i < 60; i++) {
            newSymbols.add("NEWSTOCK" + i);
            newWeights.add(BigDecimal.valueOf(3.33));
        }

        BigDecimal turnover = constraintService.calculateTurnover(currentHoldings, newSymbols, newWeights);

        assertThat(turnover).isGreaterThan(constraints.getTurnoverCapPct());
    }

    @Test
    public void testZeroTurnover() {
        // No changes = zero turnover
        // Current holding: 100 shares @ $100 = $10,000 market value (100% of portfolio)
        Holding holding = Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("AAPL")
                .quantity(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(100)) // 100 * 100 = 10,000 (100%)
                .build();
        currentHoldings.add(holding);

        // Target: AAPL stays at 100%
        List<String> targetSymbols = List.of("AAPL");
        List<BigDecimal> targetWeights = List.of(BigDecimal.valueOf(100.0));

        BigDecimal turnover = constraintService.calculateTurnover(currentHoldings, targetSymbols, targetWeights);

        assertThat(turnover).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Helper method to create universe constituents
    private UniverseConstituent createConstituent(String symbol, String capTier, String sector, int liquidityTier) {
        return UniverseConstituent.builder()
                .id(UUID.randomUUID())
                .universeId(universeId)
                .symbol(symbol)
                .marketCapTier(capTier)
                .sector(sector)
                .liquidityTier(liquidityTier)
                .isActive(true)
                .build();
    }
}
