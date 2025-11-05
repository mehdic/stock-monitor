package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.helper.TestDataHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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

  @Autowired
  private TestDataHelper testDataHelper;

  private String portfolioId;
  private TestDataHelper.TestDataContext testContext;

  @BeforeEach
  void setUp() {
    // Create complete test data setup (portfolio, universe, constraints)
    UUID testUserId = UUID.randomUUID();
    testContext = testDataHelper.createCompleteTestSetup(testUserId);
    portfolioId = testContext.getPortfolio().getId().toString();
  }


  /**
   * Test POST /api/backtests runs backtest with parameters (T170, FR-051 to FR-053).
   *
   * <p>Request body: - portfolioId: Portfolio to backtest - startDate: Backtest start date -
   * endDate: Backtest end date - constraints: Constraint set to use
   *
   * <p>Expected response: - backtestId: UUID for tracking - status: RUNNING - estimatedCompletion:
   * Time estimate
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testRunBacktest() throws Exception {
    String requestBody = String.format(
        """
            {
              "portfolioId": "%s",
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
            """, portfolioId);

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backtestId").exists())
        .andExpect(jsonPath("$.status").value("RUNNING"))
        .andExpect(jsonPath("$.estimatedCompletion").exists());
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
  @WithMockUser(roles = "OWNER")
  public void testGetBacktestResults() throws Exception {
    UUID backtestId = UUID.randomUUID();

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
    String requestBody = String.format(
        """
            {
              "portfolioId": "%s",
              "startDate": "2022-01-01",
              "endDate": "2024-01-01",
              "constraints": {}
            }
            """, portfolioId);

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isForbidden());
  }

  /**
   * Test backtest validates date range (start before end).
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testBacktestValidatesDateRange() throws Exception {
    String requestBody = String.format(
        """
            {
              "portfolioId": "%s",
              "startDate": "2024-01-01",
              "endDate": "2022-01-01",
              "constraints": {}
            }
            """, portfolioId);

    mockMvc
        .perform(post("/api/backtests").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Start date must be before end date"));
  }
}
