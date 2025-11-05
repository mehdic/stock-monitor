package com.stockmonitor.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.ConstraintPreviewDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import com.stockmonitor.service.ConstraintPreviewService;
import com.stockmonitor.service.ConstraintService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for constraint modification workflow (T134 - US3).
 *
 * <p>Tests complete flow: preview → modify → verify next run uses new constraints
 */
public class ConstraintModificationTest extends BaseIntegrationTest {

  @Autowired private ConstraintService constraintService;

  @Autowired private ConstraintPreviewService constraintPreviewService;

  @Autowired private PortfolioRepository portfolioRepository;

  @Autowired private ConstraintSetRepository constraintSetRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private RecommendationRunRepository recommendationRunRepository;

  @Autowired private RecommendationRepository recommendationRepository;

  @Autowired private HoldingRepository holdingRepository;

  private User testUser;
  private Portfolio testPortfolio;
  private ConstraintSet defaultConstraints;

  @BeforeEach
  public void setup() {
    // Create test user
    testUser = new User();
    testUser.setEmail("constraint-test@example.com");
    testUser.setPasswordHash("hashed");
    testUser.setRole(User.UserRole.OWNER);
    testUser.setEnabled(true);
    testUser.setEmailVerified(true);
    testUser = userRepository.save(testUser);

    // Create test portfolio
    testPortfolio = new Portfolio();
    testPortfolio.setUserId(testUser.getId());
    testPortfolio.setCashBalance(BigDecimal.valueOf(100000));
    testPortfolio.setTotalMarketValue(BigDecimal.valueOf(900000));
    testPortfolio = portfolioRepository.save(testPortfolio);

    // Create default constraint set matching system defaults
    defaultConstraints = new ConstraintSet();
    defaultConstraints.setUserId(testUser.getId());
    defaultConstraints.setName("Default");
    defaultConstraints.setMaxSectorExposurePct(BigDecimal.valueOf(20.0)); // System default is 20.0
    defaultConstraints.setTurnoverCapPct(BigDecimal.valueOf(25.0)); // System default is 25.0
    defaultConstraints.setVersion(1);
    defaultConstraints = constraintSetRepository.save(defaultConstraints);

    testPortfolio.setActiveConstraintSetId(defaultConstraints.getId());
    testPortfolio = portfolioRepository.save(testPortfolio);
  }

  /**
   * Test: User modifies turnover cap and previews impact
   *
   * <p>Scenario: Navigate to Settings, modify turnover cap from 25% to 20%, click "Preview
   * impact", verify preview shows expected changes
   */
  @Test
  @DisplayName("User can preview impact of constraint changes")
  public void testConstraintPreview() {
    // Given: User has existing recommendations from previous run
    createPreviousRun();

    // When: User creates modified constraints with lower turnover cap
    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxSectorExposurePct(defaultConstraints.getMaxSectorExposurePct())
            .turnoverCapPct(BigDecimal.valueOf(20.0)) // Changed from 25%
            .build();

    // Then: Preview shows expected impact
    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(testPortfolio.getId(), modifiedConstraints);

    assertNotNull(preview);
    assertNotNull(preview.getExpectedPickCount());
    assertNotNull(preview.getExpectedTurnoverPct());
    assertNotNull(preview.getAccuracyNote());

    // Verify accuracy ranges exist
    assertNotNull(preview.getExpectedPickCountRange());
    assertTrue(preview.getExpectedPickCountRange().contains("±"));

    // Verify turnover is calculated and within reasonable range
    // With 20 existing holdings and modified turnover cap from 25% to 20%,
    // the preview should show some turnover (may be lower than 15% due to constraint)
    assertNotNull(preview.getExpectedTurnoverPct(), "Turnover should be calculated");
    assertTrue(
        preview.getExpectedTurnoverPct().compareTo(BigDecimal.ZERO) >= 0,
        "Expected turnover should be non-negative");
    assertTrue(
        preview.getExpectedTurnoverPct().compareTo(BigDecimal.valueOf(30.0)) <= 0,
        "Expected turnover should be reasonable (<= 30%)");
  }

  /**
   * Test: User saves modified constraints and verifies next run uses them
   *
   * <p>Scenario: Save changes, confirm next run uses updated constraints
   */
  @Test
  @DisplayName("Modified constraints are persisted and used in next run")
  public void testConstraintPersistence() {
    // When: User saves modified constraints
    ConstraintSetDTO updatedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(8.0)) // Changed from 10%
            .maxSectorExposurePct(BigDecimal.valueOf(25.0)) // Changed from 30%
            .turnoverCapPct(BigDecimal.valueOf(20.0)) // Changed from 25%
            .build();

    ConstraintSet savedConstraints =
        constraintService.updateConstraints(testPortfolio.getId(), updatedConstraints);

    // Then: Constraints are saved with new version
    assertNotNull(savedConstraints);
    assertEquals(2, savedConstraints.getVersion());
    assertEquals(BigDecimal.valueOf(25.0), savedConstraints.getMaxSectorExposurePct());
    assertEquals(BigDecimal.valueOf(20.0), savedConstraints.getTurnoverCapPct());

    // And: Portfolio points to new constraint set
    Portfolio updatedPortfolio = portfolioRepository.findById(testPortfolio.getId()).orElseThrow();
    assertEquals(savedConstraints.getId(), updatedPortfolio.getActiveConstraintSetId());

    // And: Next run would use these constraints
    ConstraintSet activeConstraints = constraintService.getActiveConstraints(testPortfolio.getId());
    assertEquals(savedConstraints.getId(), activeConstraints.getId());
    assertEquals(2, activeConstraints.getVersion());
  }

  /**
   * Test: User resets constraints to defaults (FR-018)
   */
  @Test
  @DisplayName("User can reset constraints to defaults")
  public void testResetToDefaults() {
    // Given: User has modified constraints
    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(5.0))
            .maxSectorExposurePct(BigDecimal.valueOf(15.0))
            .turnoverCapPct(BigDecimal.valueOf(10.0))
            .build();

    constraintService.updateConstraints(testPortfolio.getId(), modifiedConstraints);

    // When: User resets to defaults
    ConstraintSet resetConstraints = constraintService.resetPortfolioConstraintsToDefaults(testPortfolio.getId());

    // Then: Constraints match system defaults (from ConstraintSetDTO.getDefaults())
    assertNotNull(resetConstraints);
    assertEquals(0, BigDecimal.valueOf(20.0).compareTo(resetConstraints.getMaxSectorExposurePct())); // System default - use compareTo for BigDecimal
    assertEquals(0, BigDecimal.valueOf(25.0).compareTo(resetConstraints.getTurnoverCapPct()));
  }

  /**
   * Helper: Create a previous recommendation run for preview comparison
   */
  private void createPreviousRun() {
    RecommendationRun previousRun = new RecommendationRun();
    previousRun.setUserId(testUser.getId());
    previousRun.setUniverseId(UUID.randomUUID());
    previousRun.setConstraintSetId(defaultConstraints.getId());
    previousRun.setScheduledDate(LocalDate.now().minusDays(1));
    previousRun.setRunType("SCHEDULED");
    previousRun.setStatus("FINALIZED");
    previousRun.setStartedAt(LocalDateTime.now().minusDays(1));
    previousRun.setCompletedAt(LocalDateTime.now().minusDays(1));
    previousRun.setDataFreshnessCheckPassed(true);
    previousRun = recommendationRunRepository.save(previousRun);

    // Create sample recommendations
    for (int i = 0; i < 30; i++) {
      Recommendation rec = new Recommendation();
      rec.setRunId(previousRun.getId());
      rec.setSymbol("TEST" + i);
      rec.setRank(i + 1);
      rec.setTargetWeightPct(BigDecimal.valueOf(100.0 / 30));
      rec.setCurrentWeightPct(BigDecimal.ZERO);
      rec.setWeightChangePct(BigDecimal.valueOf(100.0 / 30));
      rec.setConfidenceScore(75);
      rec.setExpectedCostBps(BigDecimal.valueOf(10));
      rec.setExpectedAlphaBps(BigDecimal.valueOf(50));
      rec.setEdgeOverCostBps(BigDecimal.valueOf(40));
      rec.setDriver1Name("Value");
      rec.setDriver1Score(BigDecimal.valueOf(1.5));
      rec.setDriver2Name("Momentum");
      rec.setDriver2Score(BigDecimal.valueOf(1.2));
      rec.setDriver3Name("Quality");
      rec.setDriver3Score(BigDecimal.valueOf(0.8));
      rec.setExplanation("Test explanation");
      rec.setChangeIndicator("NEW");
      rec.setSector("Technology");
      rec.setMarketCapTier("LARGE");
      rec.setLiquidityTier(1);
      rec.setCurrentPrice(BigDecimal.valueOf(100.0));
      recommendationRepository.save(rec);
    }

    // Create holdings for the first 20 stocks to enable turnover calculation
    // This represents a portfolio that already holds 20 positions
    BigDecimal totalMarketValue = testPortfolio.getTotalMarketValue();
    BigDecimal positionSize = totalMarketValue.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);

    for (int i = 0; i < 20; i++) {
      Holding holding = new Holding();
      holding.setPortfolioId(testPortfolio.getId());
      holding.setSymbol("TEST" + i);
      holding.setQuantity(BigDecimal.valueOf(10)); // 10 shares
      holding.setCurrentPrice(BigDecimal.valueOf(100.0));
      holding.setCurrentMarketValue(positionSize);
      BigDecimal costBasis = positionSize.multiply(BigDecimal.valueOf(0.9)); // 10% gain
      holding.setCostBasis(costBasis);
      holding.setCostBasisPerShare(costBasis.divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP)); // Required field
      holding.setAcquisitionDate(LocalDate.now().minusDays(30)); // Required field
      holding.setCurrency("USD"); // Required field
      holding.setWeightPct(BigDecimal.valueOf(100.0 / 20)); // Equal weight
      holding.setSector("Technology");
      holding.setMarketCapTier("LARGE");
      holdingRepository.save(holding);
    }
  }
}
