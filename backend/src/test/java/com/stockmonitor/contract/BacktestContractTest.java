package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmonitor.repository.ConstraintSetRepository;
import com.stockmonitor.repository.PortfolioRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for backtest endpoints (T170, T171).
 *
 * <p>Tests API contracts for: - POST /api/backtests - Run backtest - GET /api/backtests/{id} -
 * Get backtest results
 */
public class BacktestContractTest extends BaseIntegrationTest {

  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private ConstraintSetRepository constraintSetRepository;

  private String portfolioId;
  private UUID userId;
  private UUID backtestId;
  private UUID universeId;

  @BeforeEach
  void setUp() {
    // Create test universe
    com.stockmonitor.model.Universe universe = testDataHelper.createTestUniverse("S&P 500");
    universeId = universe.getId();

    // Create test user
    userRepository.findByEmail("testuser@example.com")
        .orElseGet(() -> testDataHelper.createTestUser("testuser@example.com"));

    userId = userRepository.findByEmail("testuser@example.com").orElseThrow().getId();

    // Create test portfolio
    portfolioId = "00000000-0000-0000-0000-000000000001";
    testDataHelper.createTestPortfolio(UUID.fromString(portfolioId), userId);

    // Create test backtest for testGetBacktestResults
    backtestId = UUID.randomUUID();
    testDataHelper.createTestBacktest(
        backtestId,
        userId,
        universeId,
        java.time.LocalDate.of(2022, 1, 1),
        java.time.LocalDate.of(2024, 1, 1)
    );
  }

  @AfterEach
  void cleanupTestData() {
    // Cleanup test data
    portfolioRepository.deleteAll();
    userRepository.deleteAll();
  }


  /**
   * Test POST /api/backtests runs backtest with parameters (T170, FR-051 to FR-053).
   *
   * <p>Request body: - portfolioId: Portfolio to backtest - universeId: Universe to use -
   * constraintSetId: Constraint set ID - name: Backtest name - startDate: Backtest start date -
   * endDate: Backtest end date - constraints: Constraint set to use
   *
   * <p>Expected response (202 Accepted): - backtestId: UUID for tracking - status: PENDING (async
   * job queued) - message: Polling instructions
   */
  @Test
  @WithMockUser(username = "testuser@example.com", roles = "OWNER")
  public void testRunBacktest() throws Exception {
    String requestBody =
        String.format(
            """
            {
              "portfolioId": "00000000-0000-0000-0000-000000000001",
              "universeId": "%s",
              "constraintSetId": "%s",
              "name": "Test Backtest",
              "startDate": "2022-01-01",
              "endDate": "2024-01-01",
              "constraints": {
                "maxPositionSizePct": 10.0,
                "maxSectorExposurePct": 25.0,
                "maxTurnoverPct": 50.0,
                "minMarketCapBn": 2.0,
                "cashBufferPct": 5.0,
                "minLiquidityTier": 2
              }
            }
            """,
            universeId,
            getConstraintSetId());

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isAccepted()) // 202 Accepted (async job started)
        .andExpect(jsonPath("$.backtestId").exists())
        // Status can be PENDING, RUNNING, or COMPLETED (stub executes instantly in tests)
        // In production with real backtest (5-120s), status will always be PENDING here
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  /**
   * Get constraint set ID for test user.
   */
  private UUID getConstraintSetId() {
    return constraintSetRepository
        .findByUserIdAndIsActiveTrue(userId)
        .map(com.stockmonitor.model.ConstraintSet::getId)
        .orElseThrow(() -> new IllegalStateException("No active constraint set for user"));
  }

  /**
   * Test GET /api/backtests/{id} returns backtest results (T171, FR-051 to FR-053).
   *
   * <p>Expected response: - backtestId - status: COMPLETED | RUNNING | FAILED - startDate, endDate
   * - Performance metrics: CAGR, Sharpe ratio, max drawdown - Equity curve: Array of {date,
   * value} - Turnover stats: Average turnover, trade count - Benchmark comparison: Portfolio vs
   * S&P 500 - Verdict: Yes/No on beating equal weight after costs
   */
  @Test
  @WithMockUser(username = "testuser@example.com", roles = "OWNER")
  public void testGetBacktestResults() throws Exception {
    mockMvc
        .perform(get("/api/backtests/{id}", backtestId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backtestId").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.startDate").exists())
        .andExpect(jsonPath("$.endDate").exists())
        .andExpect(jsonPath("$.cagr").exists())
        .andExpect(jsonPath("$.sharpeRatio").exists())
        .andExpect(jsonPath("$.maxDrawdown").exists())
        .andExpect(jsonPath("$.equityCurve").isArray())
        .andExpect(jsonPath("$.averageTurnover").exists())
        .andExpect(jsonPath("$.tradeCount").exists())
        .andExpect(jsonPath("$.benchmarkCAGR").exists())
        .andExpect(jsonPath("$.beatEqualWeight").isBoolean());
  }

  /**
   * Test backtest requires OWNER role (VIEWER cannot run backtests).
   */
  @Test
  @WithMockUser(roles = "VIEWER")
  public void testBacktestRequiresOwnerRole() throws Exception {
    String requestBody =
        """
            {
              "portfolioId": "00000000-0000-0000-0000-000000000001",
              "startDate": "2022-01-01",
              "endDate": "2024-01-01",
              "constraints": {}
            }
            """;

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isForbidden());
  }

  /**
   * Test backtest validates date range (start before end).
   */
  @Test
  @WithMockUser(username = "testuser@example.com", roles = "OWNER")
  public void testBacktestValidatesDateRange() throws Exception {
    String requestBody =
        String.format(
            """
            {
              "portfolioId": "00000000-0000-0000-0000-000000000001",
              "universeId": "%s",
              "constraintSetId": "%s",
              "name": "Test Backtest",
              "startDate": "2024-01-01",
              "endDate": "2022-01-01",
              "constraints": {}
            }
            """,
            universeId,
            getConstraintSetId());

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Start date must be before end date"));
  }

  /**
   * SECURITY TEST: GET /api/backtests/{id} returns 403 when user doesn't own the backtest.
   *
   * <p>Tests that users cannot access backtests owned by other users, preventing unauthorized
   * data access.
   */
  @Test
  @WithMockUser(username = "otheruser@example.com", roles = "OWNER")
  public void testGetBacktest_whenUserDoesNotOwnBacktest_returns403() throws Exception {
    // Given: backtestId is owned by testuser@example.com (created in setUp)
    // When: otheruser@example.com tries to access it
    // Then: Should return 403 Forbidden

    // Create other user to authenticate as
    testDataHelper.createTestUser("otheruser@example.com");

    mockMvc
        .perform(get("/api/backtests/{id}", backtestId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You don't have permission to access this backtest"));
  }

  /**
   * SECURITY TEST: POST /api/backtests returns 403 when user doesn't own the portfolio.
   *
   * <p>Tests that users cannot create backtests for portfolios owned by other users, preventing
   * unauthorized resource usage.
   */
  @Test
  @WithMockUser(username = "otheruser@example.com", roles = "OWNER")
  public void testCreateBacktest_whenUserDoesNotOwnPortfolio_returns403() throws Exception {
    // Given: portfolioId is owned by testuser@example.com (created in setUp)
    // When: otheruser@example.com tries to create backtest for it
    // Then: Should return 403 Forbidden

    // Create other user to authenticate as
    testDataHelper.createTestUser("otheruser@example.com");

    String requestBody =
        String.format(
            """
            {
              "portfolioId": "00000000-0000-0000-0000-000000000001",
              "universeId": "%s",
              "constraintSetId": "%s",
              "name": "Test Backtest",
              "startDate": "2022-01-01",
              "endDate": "2024-01-01",
              "constraints": {
                "maxPositionSizePct": 10.0,
                "maxSectorExposurePct": 25.0,
                "maxTurnoverPct": 50.0,
                "minMarketCapBn": 2.0,
                "cashBufferPct": 5.0,
                "minLiquidityTier": 2
              }
            }
            """,
            universeId,
            getConstraintSetId());

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You don't have permission to create backtests for this portfolio"));
  }
}
