package com.stockmonitor.helper;

import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for creating test data in separate transactions.
 * This ensures test data is committed and visible to HTTP requests in integration tests.
 */
@Component
@RequiredArgsConstructor
public class TestDataHelper {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PortfolioRepository portfolioRepository;
  private final UniverseRepository universeRepository;
  private final ConstraintSetRepository constraintSetRepository;
  private final RecommendationRunRepository recommendationRunRepository;
  private final RecommendationRepository recommendationRepository;
  private final HoldingRepository holdingRepository;

  /**
   * Create or get a test user in a new transaction.
   * This ensures the user is committed and visible to HTTP requests.
   *
   * @param email the user email
   * @return the created or existing user
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User createTestUser(String email) {
    return userRepository.findByEmail(email)
        .orElseGet(() -> {
          User user = User.builder()
              .email(email)
              .passwordHash(passwordEncoder.encode("password"))
              .firstName("Test")
              .lastName("User")
              .enabled(true)
              .role(User.UserRole.OWNER)
              .build();
          return userRepository.save(user);
        });
  }

  /**
   * Create a complete test setup with portfolio, universe, constraints, and holdings.
   *
   * @param userId User ID to associate entities with
   * @return TestDataContext with all created entities
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TestDataContext createCompleteTestSetup(UUID userId) {
    // 1. Create Universe
    Universe universe = Universe.builder()
        .userId(userId)
        .name("Test Universe")
        .description("Test universe for contract tests")
        .stockList("AAPL,MSFT,GOOGL,AMZN,TSLA,NVDA,META,BRK.B,JPM,JNJ")
        .isActive(true)
        .build();
    universe = universeRepository.save(universe);

    // 2. Create ConstraintSet
    ConstraintSet constraintSet = ConstraintSet.builder()
        .userId(userId)
        .name("Test Constraints")
        .description("Test constraints for contract tests")
        .maxPositionSizePct(BigDecimal.valueOf(10.0))
        .maxSectorExposurePct(BigDecimal.valueOf(25.0))
        .maxTurnoverPct(BigDecimal.valueOf(50.0))
        .minMarketCapBn(BigDecimal.valueOf(2.0))
        .cashBufferPct(BigDecimal.valueOf(5.0))
        .minLiquidityTier(2)
        .isActive(true)
        .build();
    constraintSet = constraintSetRepository.save(constraintSet);

    // 3. Create Portfolio
    Portfolio portfolio = Portfolio.builder()
        .userId(userId)
        .name("Test Portfolio")
        .description("Test portfolio for contract tests")
        .universeId(universe.getId())
        .constraintSetId(constraintSet.getId())
        .totalValueUsd(BigDecimal.valueOf(1000000))
        .cashUsd(BigDecimal.valueOf(50000))
        .isActive(true)
        .build();
    portfolio = portfolioRepository.save(portfolio);

    // 4. Create Holdings
    List<Holding> holdings = createTestHoldings(portfolio.getId(), userId);

    return TestDataContext.builder()
        .userId(userId)
        .portfolio(portfolio)
        .universe(universe)
        .constraintSet(constraintSet)
        .holdings(holdings)
        .build();
  }

  /**
   * Create a completed recommendation run with sample recommendations.
   *
   * @param context Test data context with required entities
   * @return Created RecommendationRun with recommendations
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RecommendationRun createCompletedRunWithRecommendations(TestDataContext context) {
    // Create run
    RecommendationRun run = RecommendationRun.builder()
        .userId(context.getUserId())
        .universeId(context.getUniverse().getId())
        .constraintSetId(context.getConstraintSet().getId())
        .runType("SCHEDULED")
        .status("COMPLETED")
        .scheduledDate(LocalDate.now())
        .startedAt(LocalDateTime.now().minusHours(1))
        .completedAt(LocalDateTime.now())
        .dataFreshnessCheckPassed(true)
        .constraintFeasibilityCheckPassed(true)
        .recommendationCount(5)
        .expectedAlphaBps(BigDecimal.valueOf(250.0))
        .estimatedCostBps(BigDecimal.valueOf(50.0))
        .decision("PENDING")
        .build();
    run = recommendationRunRepository.save(run);

    // Create sample recommendations
    createSampleRecommendations(run.getId(), context);

    return run;
  }

  /**
   * Create sample recommendations for a run.
   */
  private void createSampleRecommendations(UUID runId, TestDataContext context) {
    String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "NVDA"};
    String[] sectors = {"Technology", "Technology", "Technology", "Consumer", "Technology"};
    int[] ranks = {1, 2, 3, 4, 5};

    for (int i = 0; i < symbols.length; i++) {
      Recommendation rec = Recommendation.builder()
          .runId(runId)
          .userId(context.getUserId())
          .rank(ranks[i])
          .symbol(symbols[i])
          .currentWeightPct(BigDecimal.valueOf(5.0))
          .targetWeightPct(BigDecimal.valueOf(8.0 - i * 0.5))
          .weightChangePct(BigDecimal.valueOf(3.0 - i * 0.5))
          .confidenceScore(BigDecimal.valueOf(0.85 - i * 0.05))
          .expectedAlphaBps(BigDecimal.valueOf(300 - i * 20))
          .expectedCostBps(BigDecimal.valueOf(50 + i * 5))
          .edgeOverCostBps(BigDecimal.valueOf(250 - i * 25))
          .sector(sectors[i])
          .marketCapTier(1)
          .liquidityTier(1)
          .explanation("Strong factor scores across value, momentum, and quality metrics")
          .changeIndicator("UPGRADE")
          .build();
      recommendationRepository.save(rec);
    }
  }

  /**
   * Create test holdings for a portfolio.
   */
  private List<Holding> createTestHoldings(UUID portfolioId, UUID userId) {
    List<Holding> holdings = new ArrayList<>();
    String[] symbols = {"AAPL", "MSFT", "GOOGL"};
    double[] quantities = {100, 150, 50};
    double[] prices = {175.50, 380.25, 140.75};

    for (int i = 0; i < symbols.length; i++) {
      Holding holding = Holding.builder()
          .portfolioId(portfolioId)
          .userId(userId)
          .symbol(symbols[i])
          .quantity(BigDecimal.valueOf(quantities[i]))
          .averageCostPerShare(BigDecimal.valueOf(prices[i]))
          .currentPrice(BigDecimal.valueOf(prices[i] * 1.1))
          .marketValue(BigDecimal.valueOf(quantities[i] * prices[i] * 1.1))
          .unrealizedGainLoss(BigDecimal.valueOf(quantities[i] * prices[i] * 0.1))
          .unrealizedGainLossPct(BigDecimal.valueOf(10.0))
          .weightPct(BigDecimal.valueOf(100.0 / symbols.length))
          .lastUpdated(LocalDateTime.now())
          .build();
      holdings.add(holdingRepository.save(holding));
    }

    return holdings;
  }

  /**
   * Context object holding all test data entities.
   */
  @lombok.Builder
  @lombok.Data
  public static class TestDataContext {
    private UUID userId;
    private Portfolio portfolio;
    private Universe universe;
    private ConstraintSet constraintSet;
    private List<Holding> holdings;
  }
}
