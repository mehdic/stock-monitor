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
  private final UniverseConstituentRepository universeConstituentRepository;
  private final FactorModelVersionRepository factorModelVersionRepository;
  private final ObjectMapper objectMapper;
  private final jakarta.persistence.EntityManager entityManager;

  public PortfolioRepository getPortfolioRepository() {
    return portfolioRepository;
  }

  /**
   * Get the ID of the first universe (for tests that need a valid universe).
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID getFirstUniverseId() {
    return universeRepository.findAll().stream().findFirst()
        .map(com.stockmonitor.model.Universe::getId)
        .orElseThrow(() -> new IllegalStateException("No universes found - TestDataInitializer may not have run"));
  }

  /**
   * Create portfolio with specific universe ID.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Portfolio createTestPortfolioWithUniverse(UUID portfolioId, UUID userId, UUID universeId) {
    // First check if portfolio exists by portfolioId
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseGet(() -> {
          // Delete any existing portfolio for this user to avoid unique constraint violation
          portfolioRepository.findByUserId(userId).ifPresent(existing -> {
            // Delete holdings first (foreign key constraint)
            holdingRepository.deleteByPortfolioId(existing.getId());
            portfolioRepository.delete(existing);
            entityManager.flush(); // Ensure deletion is persisted before creating new one
          });

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

          LocalDateTime now = LocalDateTime.now();

          // Use native SQL to insert portfolio with specific ID (to bypass @GeneratedValue)
          entityManager.createNativeQuery(
              "INSERT INTO portfolio (id, user_id, cash_balance, total_market_value, total_cost_basis, " +
              "unrealized_pnl, unrealized_pnl_pct, benchmark_return_pct, relative_return_pct, " +
              "universe_coverage_pct, active_universe_id, active_constraint_set_id, last_calculated_at, " +
              "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
              .setParameter(1, portfolioId)
              .setParameter(2, userId)
              .setParameter(3, BigDecimal.valueOf(100000.00))
              .setParameter(4, BigDecimal.valueOf(50000.00))
              .setParameter(5, BigDecimal.ZERO)
              .setParameter(6, BigDecimal.ZERO)
              .setParameter(7, BigDecimal.ZERO)
              .setParameter(8, BigDecimal.ZERO)
              .setParameter(9, BigDecimal.ZERO)
              .setParameter(10, BigDecimal.ZERO)
              .setParameter(11, universeId)
              .setParameter(12, savedConstraintSet.getId())
              .setParameter(13, now)
              .setParameter(14, now)
              .setParameter(15, now)
              .executeUpdate();

          // Return the portfolio by finding it (now it should exist with the correct ID)
          return portfolioRepository.findById(portfolioId).orElseThrow();
        });

    // Flush and clear to ensure data is persisted and visible to other transactions
    entityManager.flush();
    entityManager.clear();
    return portfolio;
  }

  /**
   * Create or get a test user in a new transaction.
   * This ensures the user is committed and visible to HTTP requests.
   *
   * @param email the user email
   * @return the created or existing user
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User createTestUser(String email) {
    User user = createTestUserWithRole(email, User.UserRole.OWNER);
    entityManager.flush();
    entityManager.clear();
    return user;
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
    // Get a valid universe ID
    UUID universeId = getFirstUniverseId();
    return createTestPortfolioWithUniverse(portfolioId, userId, universeId);
  }

  /**
   * Check if portfolio exists.
   *
   * @param portfolioId the portfolio UUID
   * @return true if portfolio exists
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean portfolioExists(UUID portfolioId) {
    boolean exists = portfolioRepository.findById(portfolioId).isPresent();
    entityManager.flush();
    entityManager.clear();
    return exists;
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

          UUID universeId = getFirstUniverseId();
          LocalDateTime now = LocalDateTime.now();
          LocalDate today = LocalDate.now();

          // Use native SQL to insert with specific ID (bypass @GeneratedValue)
          entityManager.createNativeQuery(
              "INSERT INTO recommendation_run (id, user_id, portfolio_id, constraint_set_id, universe_id, " +
              "run_type, status, scheduled_date, started_at, completed_at, execution_duration_ms, " +
              "recommendation_count, exclusion_count, expected_turnover_pct, data_freshness_check_passed, " +
              "constraint_feasibility_check_passed, decision, created_at, updated_at) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
              .setParameter(1, runId)
              .setParameter(2, userId)
              .setParameter(3, portfolioId)
              .setParameter(4, constraintSet.getId())
              .setParameter(5, universeId)
              .setParameter(6, "SCHEDULED")
              .setParameter(7, "COMPLETED")
              .setParameter(8, today)
              .setParameter(9, now.minusHours(1))
              .setParameter(10, now.minusMinutes(30))
              .setParameter(11, 1800000L)
              .setParameter(12, 2)
              .setParameter(13, 5)
              .setParameter(14, BigDecimal.valueOf(15.0))
              .setParameter(15, true)
              .setParameter(16, true)
              .setParameter(17, "APPROVED")
              .setParameter(18, now)
              .setParameter(19, now)
              .executeUpdate();

          // Create some test recommendations for this run
          createTestRecommendation(runId, "AAPL", 1, "Technology");
          createTestRecommendation(runId, "MSFT", 2, "Technology");

          // Flush and clear to ensure data is persisted and visible to other transactions
          entityManager.flush();
          entityManager.clear();

          return recommendationRunRepository.findById(runId).orElseThrow();
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

          UUID universeId = getFirstUniverseId();
          LocalDateTime now = LocalDateTime.now();
          LocalDate today = LocalDate.now();

          // Use native SQL to insert with specific ID (bypass @GeneratedValue)
          entityManager.createNativeQuery(
              "INSERT INTO recommendation_run (id, user_id, portfolio_id, constraint_set_id, universe_id, " +
              "run_type, status, scheduled_date, started_at, recommendation_count, exclusion_count, " +
              "data_freshness_check_passed, constraint_feasibility_check_passed, decision, created_at, updated_at) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
              .setParameter(1, runId)
              .setParameter(2, userId)
              .setParameter(3, portfolioId)
              .setParameter(4, constraintSet.getId())
              .setParameter(5, universeId)
              .setParameter(6, "SCHEDULED")
              .setParameter(7, "RUNNING")
              .setParameter(8, today)
              .setParameter(9, now.minusMinutes(5))
              .setParameter(10, 0)
              .setParameter(11, 0)
              .setParameter(12, true)
              .setParameter(13, true)
              .setParameter(14, "PENDING")
              .setParameter(15, now)
              .setParameter(16, now)
              .executeUpdate();

          // Flush and clear to ensure data is persisted and visible to other transactions
          entityManager.flush();
          entityManager.clear();

          return recommendationRunRepository.findById(runId).orElseThrow();
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
          // Use native SQL to insert recommendation (bypass @GeneratedValue)
          UUID recId = UUID.randomUUID();
          entityManager.createNativeQuery(
              "INSERT INTO recommendation (id, run_id, symbol, rank, target_weight_pct, current_weight_pct, " +
              "weight_change_pct, confidence_score, expected_cost_bps, expected_alpha_bps, edge_over_cost_bps, " +
              "driver1_name, driver1_score, driver2_name, driver2_score, driver3_name, driver3_score, " +
              "explanation, change_indicator, sector, market_cap_tier, liquidity_tier, current_price, created_at) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
              .setParameter(1, recId)
              .setParameter(2, runId)
              .setParameter(3, symbol)
              .setParameter(4, rank)
              .setParameter(5, BigDecimal.valueOf(3.5))
              .setParameter(6, BigDecimal.valueOf(2.0))
              .setParameter(7, BigDecimal.valueOf(1.5))
              .setParameter(8, 85)
              .setParameter(9, BigDecimal.valueOf(15.0))
              .setParameter(10, BigDecimal.valueOf(50.0))
              .setParameter(11, BigDecimal.valueOf(35.0))
              .setParameter(12, "Value")
              .setParameter(13, BigDecimal.valueOf(0.8))
              .setParameter(14, "Momentum")
              .setParameter(15, BigDecimal.valueOf(0.7))
              .setParameter(16, "Quality")
              .setParameter(17, BigDecimal.valueOf(0.9))
              .setParameter(18, "Strong fundamental and technical indicators")
              .setParameter(19, "NEW")
              .setParameter(20, sector)
              .setParameter(21, "LARGE")
              .setParameter(22, 1)
              .setParameter(23, BigDecimal.valueOf(150.00))
              .setParameter(24, LocalDateTime.now())
              .executeUpdate();

          // Flush and clear to ensure data is persisted and visible to other transactions
          entityManager.flush();
          entityManager.clear();

          return recommendationRepository.findById(recId).orElseThrow();
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
    Universe universe = universeRepository.findByName(name)
        .orElseGet(() -> {
          try {
            Universe newUniverse = Universe.builder()
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
            return universeRepository.save(newUniverse);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create test universe", e);
          }
        });
    // Flush and clear to ensure data is persisted and visible to other transactions
    entityManager.flush();
    entityManager.clear();
    return universe;
  }

  /**
   * Save a universe in a new transaction with a specific ID, ensuring visibility to HTTP requests.
   * Use this for test data that needs to be visible across transaction boundaries.
   * Uses native SQL to bypass @GeneratedValue and set a specific UUID.
   *
   * @param universe the universe to save (with ID pre-set)
   * @return the saved universe
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Universe saveUniverseInNewTransaction(Universe universe) {
    // Use native SQL to insert with specific ID (bypass @GeneratedValue)
    LocalDateTime now = LocalDateTime.now();

    entityManager.createNativeQuery(
        "INSERT INTO universe (id, name, description, type, benchmark_symbol, constituent_count, " +
        "min_market_cap, max_market_cap, liquidity_tier_threshold, effective_date, is_active, version, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .setParameter(1, universe.getId())
        .setParameter(2, universe.getName())
        .setParameter(3, universe.getDescription())
        .setParameter(4, universe.getType())
        .setParameter(5, universe.getBenchmarkSymbol())
        .setParameter(6, universe.getConstituentCount())
        .setParameter(7, universe.getMinMarketCap())
        .setParameter(8, universe.getMaxMarketCap())
        .setParameter(9, universe.getLiquidityTierThreshold())
        .setParameter(10, universe.getEffectiveDate())
        .setParameter(11, universe.getIsActive())
        .setParameter(12, universe.getVersion())
        .setParameter(13, now)
        .setParameter(14, now)
        .executeUpdate();

    entityManager.flush();
    entityManager.clear();

    return universeRepository.findById(universe.getId()).orElseThrow();
  }

  /**
   * Save a universe constituent in a new transaction.
   *
   * @param constituent the constituent to save
   * @return the saved constituent
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UniverseConstituent saveConstituentInNewTransaction(UniverseConstituent constituent) {
    UniverseConstituent saved = universeConstituentRepository.save(constituent);
    entityManager.flush();
    entityManager.clear();
    return saved;
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

  /**
   * Create a test backtest with specific ID in a new transaction.
   *
   * @param backtestId the backtest ID
   * @param userId the user ID
   * @param universeId the universe ID
   * @param startDate the start date
   * @param endDate the end date
   * @return the created backtest
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public com.stockmonitor.model.Backtest createTestBacktest(UUID backtestId, UUID userId, UUID universeId, LocalDate startDate, LocalDate endDate) {
    // Create constraint set for backtest
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

    LocalDateTime now = LocalDateTime.now();

    // Use native SQL to insert with specific ID
    entityManager.createNativeQuery(
        "INSERT INTO backtest (id, user_id, universe_id, constraint_set_id, name, start_date, end_date, " +
        "initial_capital, final_value, total_return_pct, cagr_pct, volatility_pct, sharpe_ratio, " +
        "max_drawdown_pct, hit_rate_pct, avg_turnover_pct, total_cost_bps, " +
        "benchmark_return_pct, beat_equal_weight, status, equity_curve_data, turnover_history, cost_assumptions, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .setParameter(1, backtestId)
        .setParameter(2, userId)
        .setParameter(3, universeId)
        .setParameter(4, constraintSet.getId())
        .setParameter(5, "Test Backtest")
        .setParameter(6, startDate)
        .setParameter(7, endDate)
        .setParameter(8, BigDecimal.valueOf(1000000.00))
        .setParameter(9, BigDecimal.valueOf(1250000.00))
        .setParameter(10, BigDecimal.valueOf(25.00))
        .setParameter(11, BigDecimal.valueOf(12.50))
        .setParameter(12, BigDecimal.valueOf(18.20))
        .setParameter(13, BigDecimal.valueOf(0.89))
        .setParameter(14, BigDecimal.valueOf(-15.30))
        .setParameter(15, BigDecimal.valueOf(58.30))
        .setParameter(16, BigDecimal.valueOf(22.80))
        .setParameter(17, BigDecimal.valueOf(54.32))
        .setParameter(18, BigDecimal.valueOf(10.20))
        .setParameter(19, true)
        .setParameter(20, "COMPLETED")
        .setParameter(21, "[]")
        .setParameter(22, "[]")
        .setParameter(23, "{}")
        .setParameter(24, now)
        .setParameter(25, now)
        .executeUpdate();

    // Flush and clear to ensure data is persisted
    entityManager.flush();
    entityManager.clear();

    return entityManager.find(com.stockmonitor.model.Backtest.class, backtestId);
  }
}
