package com.stockmonitor.helper;

import com.stockmonitor.model.*;
import com.stockmonitor.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

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
  private final HoldingRepository holdingRepository;
  private final RecommendationRunRepository recommendationRunRepository;
  private final RecommendationRepository recommendationRepository;
  private final ConstraintSetRepository constraintSetRepository;
  private final UniverseRepository universeRepository;
  private final FactorModelVersionRepository factorModelVersionRepository;
  private final ObjectMapper objectMapper;

  /**
   * Create or get a test user in a new transaction.
   * This ensures the user is committed and visible to HTTP requests.
   *
   * @param email the user email
   * @return the created or existing user
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User createTestUser(String email) {
    return createTestUserWithRole(email, User.UserRole.OWNER);
  }

  /**
   * Create or get a test user with specific role in a new transaction.
   *
   * @param email the user email
   * @param role the user role
   * @return the created or existing user
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User createTestUserWithRole(String email, User.UserRole role) {
    return userRepository.findByEmail(email)
        .orElseGet(() -> {
          User user = User.builder()
              .email(email)
              .passwordHash(passwordEncoder.encode("password"))
              .firstName("Test")
              .lastName("User")
              .enabled(true)
              .role(role)
              .build();
          return userRepository.save(user);
        });
  }

  /**
   * Create a test portfolio in a new transaction.
   * Idempotent: returns existing portfolio if one exists with same userId (due to unique constraint).
   *
   * @param portfolioId the portfolio UUID
   * @param userId the user ID
   * @return the created portfolio
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Portfolio createTestPortfolio(UUID portfolioId, UUID userId) {
    // First check if portfolio exists by portfolioId
    return portfolioRepository.findById(portfolioId)
        .orElseGet(() -> {
          // Check if a portfolio already exists for this userId (due to unique constraint)
          return portfolioRepository.findByUserId(userId)
              .orElseGet(() -> {
                // Create constraint set for portfolio
                ConstraintSet constraintSet = ConstraintSet.builder()
                    .userId(userId)
                    .name("Default Constraints")
                    .isActive(true)
                    .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
                    .maxNameWeightMidCapPct(BigDecimal.valueOf(5.0))
                    .maxNameWeightSmallCapPct(BigDecimal.valueOf(2.0))
                    .maxSectorExposurePct(BigDecimal.valueOf(30.0))
                    .turnoverCapPct(BigDecimal.valueOf(25.0))
                    .version(1)
                    .build();
                ConstraintSet savedConstraintSet = constraintSetRepository.save(constraintSet);

                Portfolio portfolio = Portfolio.builder()
                    .id(portfolioId)
                    .userId(userId)
                    .cashBalance(BigDecimal.valueOf(100000.00))
                    .totalMarketValue(BigDecimal.valueOf(50000.00))
                    .activeConstraintSetId(savedConstraintSet.getId())
                    .activeUniverseId(UUID.randomUUID()) // Set a default universe
                    .build();
                return portfolioRepository.save(portfolio);
              });
        });
  }

  /**
   * Create a test holding in a new transaction.
   * Idempotent: returns existing holding if one exists with same portfolioId and symbol.
   *
   * @param portfolioId the portfolio UUID
   * @param symbol the stock symbol
   * @param quantity the quantity
   * @param costBasis the cost basis per share
   * @param sector the sector
   * @return the created or existing holding
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Holding createTestHolding(UUID portfolioId, String symbol, BigDecimal quantity,
      BigDecimal costBasisTotal, String sector) {
    return holdingRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
        .orElseGet(() -> {
          BigDecimal costBasisPerShare = costBasisTotal.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
          BigDecimal currentPrice = costBasisPerShare.multiply(BigDecimal.valueOf(1.1)); // 10% gain
          BigDecimal currentMarketValue = currentPrice.multiply(quantity);

          Holding holding = Holding.builder()
              .portfolioId(portfolioId)
              .symbol(symbol)
              .quantity(quantity)
              .costBasis(costBasisTotal)
              .costBasisPerShare(costBasisPerShare)
              .acquisitionDate(LocalDate.now().minusDays(30))
              .currency("USD")
              .currentPrice(currentPrice)
              .currentMarketValue(currentMarketValue)
              .sector(sector)
              .build();

          return holdingRepository.save(holding);
        });
  }

  /**
   * Create a test recommendation run in a new transaction.
   * Idempotent: returns existing run if one exists with same userId, portfolioId, and scheduled date.
   *
   * @param userId the user ID
   * @param portfolioId the portfolio ID
   * @return the created or existing recommendation run
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RecommendationRun createTestRecommendationRun(UUID userId, UUID portfolioId) {
    LocalDate today = LocalDate.now();

    return recommendationRunRepository.findByUserIdAndPortfolioIdAndScheduledDate(userId, portfolioId, today)
        .orElseGet(() -> {
          // Create or get constraint set
          ConstraintSet constraintSet = constraintSetRepository
              .findByUserIdAndIsActiveTrue(userId)
              .orElseGet(() -> {
                ConstraintSet cs = ConstraintSet.builder()
                    .userId(userId)
                    .name("Test Constraints")
                    .isActive(true)
                    .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
                    .maxSectorExposurePct(BigDecimal.valueOf(30.0))
                    .turnoverCapPct(BigDecimal.valueOf(25.0))
                    .version(1)
                    .build();
                return constraintSetRepository.save(cs);
              });

          RecommendationRun run = RecommendationRun.builder()
              .userId(userId)
              .portfolioId(portfolioId)
              .constraintSetId(constraintSet.getId())
              .universeId(UUID.randomUUID()) // Mock universe ID
              .runType("MONTHLY")
              .status("FINALIZED")
              .scheduledDate(today)
              .startedAt(LocalDateTime.now().minusHours(1))
              .completedAt(LocalDateTime.now().minusMinutes(30))
              .executionDurationMs(1800000L)
              .recommendationCount(30)
              .exclusionCount(5)
              .expectedTurnoverPct(BigDecimal.valueOf(15.0))
              .build();

          return recommendationRunRepository.save(run);
        });
  }

  /**
   * Create a test recommendation run with a specific ID in a new transaction.
   *
   * @param runId the run ID
   * @param userId the user ID
   * @param portfolioId the portfolio ID
   * @return the created or existing recommendation run
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RecommendationRun createTestRecommendationRunWithId(UUID runId, UUID userId, UUID portfolioId) {
    return recommendationRunRepository.findById(runId)
        .orElseGet(() -> {
          // Create or get constraint set
          ConstraintSet constraintSet = constraintSetRepository
              .findByUserIdAndIsActiveTrue(userId)
              .orElseGet(() -> {
                ConstraintSet cs = ConstraintSet.builder()
                    .userId(userId)
                    .name("Test Constraints")
                    .isActive(true)
                    .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
                    .maxSectorExposurePct(BigDecimal.valueOf(30.0))
                    .turnoverCapPct(BigDecimal.valueOf(25.0))
                    .version(1)
                    .build();
                return constraintSetRepository.save(cs);
              });

          RecommendationRun run = RecommendationRun.builder()
              .id(runId)
              .userId(userId)
              .portfolioId(portfolioId)
              .constraintSetId(constraintSet.getId())
              .universeId(UUID.randomUUID()) // Mock universe ID
              .runType("SCHEDULED")
              .status("COMPLETED")
              .scheduledDate(LocalDate.now())
              .startedAt(LocalDateTime.now().minusHours(1))
              .completedAt(LocalDateTime.now().minusMinutes(30))
              .executionDurationMs(1800000L)
              .recommendationCount(30)
              .exclusionCount(5)
              .expectedTurnoverPct(BigDecimal.valueOf(15.0))
              .build();

          RecommendationRun savedRun = recommendationRunRepository.save(run);

          // Create some test recommendations for this run
          createTestRecommendation(runId, "AAPL", 1, "Technology");
          createTestRecommendation(runId, "MSFT", 2, "Technology");

          return savedRun;
        });
  }

  /**
   * Create a test recommendation run in progress (no recommendations yet) with a specific ID.
   *
   * @param runId the run ID
   * @param userId the user ID
   * @param portfolioId the portfolio ID
   * @return the created or existing recommendation run
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RecommendationRun createTestRecommendationRunInProgress(UUID runId, UUID userId, UUID portfolioId) {
    return recommendationRunRepository.findById(runId)
        .orElseGet(() -> {
          // Create or get constraint set
          ConstraintSet constraintSet = constraintSetRepository
              .findByUserIdAndIsActiveTrue(userId)
              .orElseGet(() -> {
                ConstraintSet cs = ConstraintSet.builder()
                    .userId(userId)
                    .name("Test Constraints")
                    .isActive(true)
                    .maxNameWeightLargeCapPct(BigDecimal.valueOf(10.0))
                    .maxSectorExposurePct(BigDecimal.valueOf(30.0))
                    .turnoverCapPct(BigDecimal.valueOf(25.0))
                    .version(1)
                    .build();
                return constraintSetRepository.save(cs);
              });

          RecommendationRun run = RecommendationRun.builder()
              .id(runId)
              .userId(userId)
              .portfolioId(portfolioId)
              .constraintSetId(constraintSet.getId())
              .universeId(UUID.randomUUID()) // Mock universe ID
              .runType("SCHEDULED")
              .status("RUNNING")
              .scheduledDate(LocalDate.now())
              .startedAt(LocalDateTime.now().minusMinutes(5))
              .recommendationCount(0)
              .build();

          return recommendationRunRepository.save(run);
        });
  }

  /**
   * Create a test recommendation in a new transaction.
   * Idempotent: returns existing recommendation if one exists with same runId and symbol.
   *
   * @param runId the recommendation run ID
   * @param symbol the stock symbol
   * @param rank the rank (1 = highest)
   * @param sector the sector
   * @return the created or existing recommendation
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Recommendation createTestRecommendation(UUID runId, String symbol, int rank, String sector) {
    return recommendationRepository.findByRunIdAndSymbol(runId, symbol)
        .orElseGet(() -> {
          Recommendation rec = Recommendation.builder()
              .runId(runId)
              .symbol(symbol)
              .rank(rank)
              .targetWeightPct(BigDecimal.valueOf(3.5))
              .currentWeightPct(BigDecimal.valueOf(2.0))
              .weightChangePct(BigDecimal.valueOf(1.5))
              .confidenceScore(85)
              .expectedCostBps(BigDecimal.valueOf(15.0))
              .expectedAlphaBps(BigDecimal.valueOf(50.0))
              .edgeOverCostBps(BigDecimal.valueOf(35.0))
              .driver1Name("Value")
              .driver1Score(BigDecimal.valueOf(0.8))
              .driver2Name("Momentum")
              .driver2Score(BigDecimal.valueOf(0.7))
              .driver3Name("Quality")
              .driver3Score(BigDecimal.valueOf(0.9))
              .explanation("Strong fundamental and technical indicators")
              .sector(sector)
              .marketCapTier("LARGE")
              .liquidityTier(1)
              .currentPrice(BigDecimal.valueOf(150.00))
              .build();

          return recommendationRepository.save(rec);
        });
  }

  /**
   * Create a test universe in a new transaction.
   * Idempotent: returns existing universe if one exists with same name.
   *
   * @param name the universe name
   * @return the created or existing universe
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Universe createTestUniverse(String name) {
    return universeRepository.findByName(name)
        .orElseGet(() -> {
          try {
            Universe universe = Universe.builder()
                .name(name)
                .type("CUSTOM")
                .benchmarkSymbol("SPY")
                .description("Test universe for integration tests")
                .effectiveDate(LocalDate.now())
                .constituentCount(100)
                .isActive(true)
                .version(1)
                .liquidityTierThreshold(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("tier1_min_adv_usd", 10000000);
                      put("tier2_min_adv_usd", 5000000);
                      put("tier3_min_adv_usd", 1000000);
                    }}
                ))
                .build();
            return universeRepository.save(universe);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create test universe", e);
          }
        });
  }

  /**
   * Create a test factor model version in a new transaction.
   * Idempotent: returns existing version if one exists with same version number.
   *
   * @param versionNumber the version number
   * @return the created or existing factor model version
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public FactorModelVersion createTestFactorModelVersion(String versionNumber) {
    return factorModelVersionRepository.findByVersionNumber(versionNumber)
        .orElseGet(() -> {
          try {
            FactorModelVersion version = FactorModelVersion.builder()
                .versionNumber(versionNumber)
                .isActive(true)
                .effectiveDate(LocalDate.now())
                .createdBy("test-system")
                .approvedBy("test-admin")
                .sectorNeutralizationMethod("Z_SCORE")
                .winsorizationPercentile(BigDecimal.valueOf(1.00))
                .description("Test factor model version")
                .valueDefinition(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("metrics", java.util.Arrays.asList("PE", "PB", "PS"));
                      put("weights", java.util.Arrays.asList(0.4, 0.3, 0.3));
                    }}
                ))
                .momentumDefinition(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("periods", java.util.Arrays.asList(1, 3, 6, 12));
                      put("weights", java.util.Arrays.asList(0.1, 0.2, 0.3, 0.4));
                    }}
                ))
                .qualityDefinition(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("metrics", java.util.Arrays.asList("ROE", "ROA", "DEBT_TO_EQUITY"));
                      put("weights", java.util.Arrays.asList(0.4, 0.3, 0.3));
                    }}
                ))
                .revisionsDefinition(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("metrics", java.util.Arrays.asList("EPS_REV_UP", "EPS_REV_DOWN"));
                      put("weights", java.util.Arrays.asList(0.6, 0.4));
                    }}
                ))
                .compositeWeighting(objectMapper.writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                      put("value", 0.25);
                      put("momentum", 0.25);
                      put("quality", 0.25);
                      put("revisions", 0.25);
                    }}
                ))
                .build();
            return factorModelVersionRepository.save(version);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create test factor model version", e);
          }
        });
  }
}
