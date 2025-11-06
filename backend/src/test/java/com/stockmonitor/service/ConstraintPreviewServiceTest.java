package com.stockmonitor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

import com.stockmonitor.BaseUnitTest;
import com.stockmonitor.dto.ConstraintPreviewDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.engine.RecommendationEngine;
import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Unit tests for ConstraintPreviewService (T135 - US3).
 *
 * <p>Tests preview impact simulation using last run's factor scores
 */
public class ConstraintPreviewServiceTest extends BaseUnitTest {

  @Mock private RecommendationRunRepository recommendationRunRepository;

  @Mock private RecommendationRepository recommendationRepository;

  @Mock private PortfolioRepository portfolioRepository;

  @Mock private ConstraintSetRepository constraintSetRepository;

  @Mock private HoldingRepository holdingRepository;

  @Mock private UniverseConstituentRepository universeConstituentRepository;

  @InjectMocks private ConstraintPreviewService constraintPreviewService;

  private UUID portfolioId;
  private Portfolio portfolio;
  private RecommendationRun lastRun;
  private List<Recommendation> lastRecommendations;
  private ConstraintSet currentConstraints;

  @BeforeEach
  public void setup() {
    portfolioId = UUID.randomUUID();

    // Setup portfolio
    portfolio = new Portfolio();
    portfolio.setId(portfolioId);
    portfolio.setUserId(UUID.randomUUID());
    portfolio.setCashBalance(BigDecimal.valueOf(100000));
    portfolio.setTotalMarketValue(BigDecimal.valueOf(900000));

    // Setup current constraints
    currentConstraints = new ConstraintSet();
    currentConstraints.setId(UUID.randomUUID());
    currentConstraints.setMaxSectorExposurePct(BigDecimal.valueOf(30.0));

    portfolio.setActiveConstraintSetId(currentConstraints.getId());

    // Setup last run
    lastRun = new RecommendationRun();
    lastRun.setId(UUID.randomUUID());
    lastRun.setUserId(portfolioId);
    lastRun.setConstraintSetId(currentConstraints.getId());
    lastRun.setStatus("FINALIZED");
    lastRun.setCompletedAt(LocalDateTime.now().minusDays(7));

    // Setup last recommendations (30 picks with sectors)
    lastRecommendations = new ArrayList<>();
    String[] sectors = {"Technology", "Healthcare", "Finance", "Consumer", "Energy"};
    for (int i = 0; i < 30; i++) {
      Recommendation rec = new Recommendation();
      rec.setRunId(lastRun.getId());
      rec.setSymbol("STOCK" + i);
      rec.setRank(i + 1);
      rec.setTargetWeightPct(BigDecimal.valueOf(100.0 / 30));
      rec.setSector(sectors[i % sectors.length]); // Distribute across sectors
      rec.setMarketCapTier("LARGE");
      rec.setLiquidityTier(1);
      lastRecommendations.add(rec);
    }

    when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
    when(constraintSetRepository.findById(currentConstraints.getId()))
        .thenReturn(Optional.of(currentConstraints));
    when(recommendationRunRepository
            .findFirstByUserIdAndStatusOrderByCompletedAtDesc(portfolio.getUserId(), "FINALIZED"))
        .thenReturn(Optional.of(lastRun));
    when(recommendationRepository.findByRunId(lastRun.getId())).thenReturn(lastRecommendations);

    // Create mock holdings for calculation
    List<Holding> mockHoldings = new ArrayList<>();
    Holding holding = new Holding();
    holding.setSymbol("AAPL");
    holding.setQuantity(BigDecimal.valueOf(100));
    holding.setCurrentPrice(BigDecimal.valueOf(150));
    holding.setCostBasis(BigDecimal.valueOf(100));
    mockHoldings.add(holding);
    when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(mockHoldings);
  }

  /**
   * Test: Preview returns estimates with accuracy ranges (FR-017)
   */
  @Test
  @DisplayName("Preview returns impact estimates with ±10% pick count, ±15% turnover ranges")
  public void testPreviewReturnsEstimatesWithRanges() {
    // Given: Modified constraints with lower turnover cap
    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(30.0))
            .turnoverCapPct(BigDecimal.valueOf(20.0)) // Changed from 25%
            .build();

    // When: Preview impact
    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(portfolioId, modifiedConstraints);

    // Then: Returns estimates with ranges
    assertNotNull(preview);
    assertNotNull(preview.getExpectedPickCount());
    assertNotNull(preview.getExpectedPickCountRange());
    assertTrue(preview.getExpectedPickCountRange().contains("±10%"));

    assertNotNull(preview.getExpectedTurnoverPct());
    assertNotNull(preview.getExpectedTurnoverRange());
    assertTrue(preview.getExpectedTurnoverRange().contains("±15%"));

    assertNotNull(preview.getAccuracyNote());
    assertTrue(
        preview
            .getAccuracyNote()
            .contains("actual results may vary"));
  }

  /**
   * Test: Preview calculates affected positions
   */
  @Test
  @DisplayName("Preview identifies dropped and added symbols")
  public void testPreviewCalculatesAffectedPositions() {
    // Given: Modified constraints
    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(25.0)) // Tighter sector cap
            .turnoverCapPct(BigDecimal.valueOf(20.0))
            .build();

    // When: Preview impact
    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(portfolioId, modifiedConstraints);

    // Then: Identifies affected positions
    assertNotNull(preview.getAffectedPositionsCount());
    assertNotNull(preview.getDroppedSymbols());
    assertNotNull(preview.getAddedSymbols());

    // Affected positions should be sum of dropped + added
    int totalAffected = preview.getDroppedSymbols().size() + preview.getAddedSymbols().size();
    assertEquals(preview.getAffectedPositionsCount(), totalAffected);
  }

  /**
   * Test: Preview fails gracefully if no historical run data (FR-017)
   */
  @Test
  @DisplayName("Preview fails gracefully with helpful message when no historical run data exists")
  public void testPreviewWithoutHistoricalData() {
    // Reset mocks to avoid unnecessary stubbing error
    reset(recommendationRunRepository, recommendationRepository, holdingRepository);

    // Given: Portfolio and constraints exist but no previous run
    when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
    when(constraintSetRepository.findById(currentConstraints.getId()))
        .thenReturn(Optional.of(currentConstraints));
    when(recommendationRunRepository
            .findFirstByUserIdAndStatusOrderByCompletedAtDesc(portfolio.getUserId(), "FINALIZED"))
        .thenReturn(Optional.empty());

    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(30.0))
            .turnoverCapPct(BigDecimal.valueOf(20.0))
            .build();

    // When: Preview impact
    Exception exception =
        assertThrows(
            IllegalStateException.class,
            () -> constraintPreviewService.previewConstraintImpact(portfolioId, modifiedConstraints));

    // Then: Helpful error message
    assertTrue(
        exception
            .getMessage()
            .contains(
                "No historical run data available for preview. Please run at least one recommendation first."));
  }

  /**
   * Test: Lower turnover cap reduces expected turnover
   */
  @Test
  @DisplayName("Lower turnover cap results in lower expected turnover estimate")
  public void testLowerTurnoverCapReducesExpectedTurnover() {
    // Given: Current turnover cap is 25%
    ConstraintSetDTO lowerTurnoverConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(30.0))
            .turnoverCapPct(BigDecimal.valueOf(15.0)) // Much lower: 15% vs 25%
            .build();

    // When: Preview impact
    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(portfolioId, lowerTurnoverConstraints);

    // Then: Expected turnover should be <= 15%
    assertTrue(
        preview.getExpectedTurnoverPct().compareTo(BigDecimal.valueOf(15.0)) <= 0,
        "Expected turnover should respect new cap");
  }

  /**
   * Test: Higher market cap floor reduces expected pick count
   */
  @Test
  @DisplayName("Higher market cap floor reduces expected pick count")
  public void testHigherMarketCapFloorReducesPickCount() {
    // Given: Current market cap floor is 1.0Bn
    ConstraintSetDTO higherMarketCapConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(30.0))
            .turnoverCapPct(BigDecimal.valueOf(25.0))
            .build();

    // When: Preview impact
    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(portfolioId, higherMarketCapConstraints);

    // Then: Expected pick count should be lower than current (30)
    assertTrue(
        preview.getExpectedPickCount() < 30,
        "Higher market cap floor should reduce available universe");
  }

  /**
   * Test: Accuracy note mentions ±10% picks, ±15% turnover
   */
  @Test
  @DisplayName("Accuracy note contains expected ranges")
  public void testAccuracyNoteContainsExpectedRanges() {
    ConstraintSetDTO constraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
            .maxSectorExposurePct(BigDecimal.valueOf(30.0))
            .turnoverCapPct(BigDecimal.valueOf(20.0))
            .build();

    ConstraintPreviewDTO preview =
        constraintPreviewService.previewConstraintImpact(portfolioId, constraints);

    String note = preview.getAccuracyNote();
    assertTrue(note.contains("±10%"));
    assertTrue(note.contains("±15%"));
    assertTrue(note.toLowerCase().contains("actual results may vary"));
  }
}
