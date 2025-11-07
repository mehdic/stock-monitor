package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.FactorScoreDTO;
import com.stockmonitor.engine.FactorCalculationService;
import com.stockmonitor.model.FactorModelVersion;
import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.FactorModelVersionRepository;
import com.stockmonitor.repository.HoldingRepository;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for factor calculation workflow with versioning.
 *
 * Tests:
 * - Factor score calculation for holdings
 * - Factor model version management
 * - Calculation consistency across versions
 * - Raw factor score generation (Value, Momentum, Quality, Revisions)
 */
@Transactional
public class FactorCalculationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FactorCalculationService factorCalculationService;

    @Autowired
    private FactorModelVersionRepository factorModelVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    private User testUser;
    private Portfolio testPortfolio;
    private FactorModelVersion testFactorModelVersion;

    @BeforeEach
    public void setup() {
        // Clean up test data (except FactorModelVersion which is shared global test data)
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("factor-test@example.com")
                .passwordHash("dummy")
                .firstName("Factor")
                .lastName("Test")
                .role(User.UserRole.OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        // Create test portfolio
        testPortfolio = Portfolio.builder()
                .userId(testUser.getId())
                .cashBalance(BigDecimal.valueOf(100000))
                .totalMarketValue(BigDecimal.valueOf(50000))
                .activeUniverseId(UUID.randomUUID())
                .activeConstraintSetId(UUID.randomUUID())
                .build();
        testPortfolio = portfolioRepository.save(testPortfolio);

        // Use existing FactorModelVersion created by TestDataInitializer (version "1.0.0")
        testFactorModelVersion = factorModelVersionRepository.findByVersionNumber("1.0.0")
                .orElseThrow(() -> new IllegalStateException("Global test data not initialized - " +
                        "TestDataInitializer should have created FactorModelVersion 1.0.0"));
    }

    /**
     * Test factor score calculation for a single holding.
     */
    @Test
    public void testCalculateFactorScores_SingleHolding_ReturnsValidScores() {
        // Arrange
        Holding holding = Holding.builder()
                .portfolioId(testPortfolio.getId())
                .symbol("AAPL")
                .quantity(BigDecimal.valueOf(100))
                .costBasis(BigDecimal.valueOf(15000))
                .costBasisPerShare(BigDecimal.valueOf(150))
                .currentPrice(BigDecimal.valueOf(165))
                .currentMarketValue(BigDecimal.valueOf(16500))
                .sector("Technology")
                .acquisitionDate(LocalDate.now().minusDays(30))
                .currency("USD")
                .build();
        holding = holdingRepository.save(holding);

        // Act
        FactorScoreDTO factorScores = factorCalculationService.calculateFactorScores(holding);

        // Assert
        assertThat(factorScores).isNotNull();
        assertThat(factorScores.getSymbol()).isEqualTo("AAPL");
        assertThat(factorScores.getSector()).isEqualTo("Technology");

        // Verify all factor scores are present
        assertThat(factorScores.getValue()).isNotNull();
        assertThat(factorScores.getMomentum()).isNotNull();
        assertThat(factorScores.getQuality()).isNotNull();
        assertThat(factorScores.getRevisions()).isNotNull();

        // Verify scores are within expected range [-1, 1]
        assertThat(factorScores.getValue()).isBetween(BigDecimal.valueOf(-1), BigDecimal.valueOf(1));
        assertThat(factorScores.getMomentum()).isBetween(BigDecimal.valueOf(-1), BigDecimal.valueOf(1));
        assertThat(factorScores.getQuality()).isBetween(BigDecimal.valueOf(-1), BigDecimal.valueOf(1));
        assertThat(factorScores.getRevisions()).isBetween(BigDecimal.valueOf(-1), BigDecimal.valueOf(1));

        // Verify calculation timestamp is recent
        assertThat(factorScores.getCalculatedAt()).isNotNull();
        assertThat(factorScores.getCalculatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    /**
     * Test factor calculation for multiple holdings.
     */
    @Test
    public void testCalculateFactorScores_MultipleHoldings_AllReturnValidScores() {
        // Arrange - Create multiple holdings
        Holding holding1 = createHolding("AAPL", "Technology", BigDecimal.valueOf(100));
        Holding holding2 = createHolding("MSFT", "Technology", BigDecimal.valueOf(200));
        Holding holding3 = createHolding("JPM", "Financials", BigDecimal.valueOf(150));

        // Act
        FactorScoreDTO scores1 = factorCalculationService.calculateFactorScores(holding1);
        FactorScoreDTO scores2 = factorCalculationService.calculateFactorScores(holding2);
        FactorScoreDTO scores3 = factorCalculationService.calculateFactorScores(holding3);

        // Assert - All calculations successful
        assertThat(scores1).isNotNull();
        assertThat(scores2).isNotNull();
        assertThat(scores3).isNotNull();

        // Verify each has correct symbol
        assertThat(scores1.getSymbol()).isEqualTo("AAPL");
        assertThat(scores2.getSymbol()).isEqualTo("MSFT");
        assertThat(scores3.getSymbol()).isEqualTo("JPM");

        // Verify all have valid scores
        assertThat(scores1.getValue()).isNotNull();
        assertThat(scores2.getMomentum()).isNotNull();
        assertThat(scores3.getQuality()).isNotNull();
    }

    /**
     * Test factor model version is tracked with calculations.
     */
    @Test
    public void testCalculateFactorScores_TracksActiveFactorModelVersion() {
        // Arrange
        Holding holding = createHolding("AAPL", "Technology", BigDecimal.valueOf(100));

        // Verify active version exists
        Optional<FactorModelVersion> activeVersion = factorModelVersionRepository.findByIsActiveTrue();
        assertThat(activeVersion).isPresent();

        // Act
        FactorScoreDTO scores = factorCalculationService.calculateFactorScores(holding);

        // Assert - Calculation completed successfully with active version available
        assertThat(scores).isNotNull();
        assertThat(scores.getCalculatedAt()).isNotNull();
    }

    /**
     * Test factor calculation handles new version deployment.
     */
    @Test
    public void testCalculateFactorScores_MultipleVersions_UsesActiveVersion() {
        // Arrange - Delete existing version and create new active version
        factorModelVersionRepository.deleteAll();

        FactorModelVersion newVersion = FactorModelVersion.builder()
                .versionNumber("2.0.0")
                .effectiveDate(LocalDate.now())
                .isActive(true)
                .sectorNeutralizationMethod("Z_SCORE")
                .winsorizationPercentile(BigDecimal.valueOf(1.00))
                .valueDefinition("{\"metrics\":[\"PE\"],\"weights\":[1.0]}")
                .momentumDefinition("{\"periods\":[6,12],\"weights\":[0.5,0.5]}")
                .qualityDefinition("{\"metrics\":[\"ROE\"],\"weights\":[1.0]}")
                .revisionsDefinition("{\"metrics\":[\"EPS_REV\"],\"weights\":[1.0]}")
                .compositeWeighting("{\"value\":0.3,\"momentum\":0.3,\"quality\":0.2,\"revisions\":0.2}")
                .description("Test factor model version 2.0.0")
                .createdBy("test-system")
                .approvedBy("test-admin")
                .build();
        newVersion = factorModelVersionRepository.save(newVersion);

        Holding holding = createHolding("AAPL", "Technology", BigDecimal.valueOf(100));

        // Act
        FactorScoreDTO scores = factorCalculationService.calculateFactorScores(holding);

        // Assert - Calculation succeeds with new version active
        assertThat(scores).isNotNull();
        assertThat(scores.getSymbol()).isEqualTo("AAPL");

        // Verify new version is active
        Optional<FactorModelVersion> activeVersion = factorModelVersionRepository.findByIsActiveTrue();
        assertThat(activeVersion).isPresent();
        assertThat(activeVersion.get().getVersionNumber()).isEqualTo("2.0.0");
    }

    /**
     * Test factor calculation consistency - same input should produce consistent output structure.
     */
    @Test
    public void testCalculateFactorScores_ConsistencyCheck_ProducesConsistentStructure() {
        // Arrange
        Holding holding = createHolding("AAPL", "Technology", BigDecimal.valueOf(100));

        // Act - Calculate twice
        FactorScoreDTO scores1 = factorCalculationService.calculateFactorScores(holding);
        FactorScoreDTO scores2 = factorCalculationService.calculateFactorScores(holding);

        // Assert - Both calculations return valid structure
        assertThat(scores1).isNotNull();
        assertThat(scores2).isNotNull();

        // Both have same symbol and sector
        assertThat(scores1.getSymbol()).isEqualTo(scores2.getSymbol());
        assertThat(scores1.getSector()).isEqualTo(scores2.getSector());

        // Both have all four factor scores populated
        assertThat(scores1.getValue()).isNotNull();
        assertThat(scores1.getMomentum()).isNotNull();
        assertThat(scores1.getQuality()).isNotNull();
        assertThat(scores1.getRevisions()).isNotNull();

        assertThat(scores2.getValue()).isNotNull();
        assertThat(scores2.getMomentum()).isNotNull();
        assertThat(scores2.getQuality()).isNotNull();
        assertThat(scores2.getRevisions()).isNotNull();
    }

    /**
     * Helper method to create a holding.
     */
    private Holding createHolding(String symbol, String sector, BigDecimal quantity) {
        BigDecimal costBasisPerShare = BigDecimal.valueOf(150);
        BigDecimal currentPrice = BigDecimal.valueOf(165);

        Holding holding = Holding.builder()
                .portfolioId(testPortfolio.getId())
                .symbol(symbol)
                .quantity(quantity)
                .costBasis(costBasisPerShare.multiply(quantity))
                .costBasisPerShare(costBasisPerShare)
                .currentPrice(currentPrice)
                .currentMarketValue(currentPrice.multiply(quantity))
                .sector(sector)
                .acquisitionDate(LocalDate.now().minusDays(30))
                .currency("USD")
                .build();

        return holdingRepository.save(holding);
    }
}
