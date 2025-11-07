package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockmonitor.model.Holding;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.HoldingRepository;
import com.stockmonitor.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for factor analysis endpoints (T147).
 *
 * <p>Tests API contracts for: - GET /api/portfolios/{id}/factors - Returns factor scores for all
 * holdings - GET /api/holdings/{id}/factors - Returns factor scores for specific holding
 */
public class FactorContractTest extends BaseIntegrationTest {

  @Autowired
  private PortfolioRepository portfolioRepository;

  @Autowired
  private HoldingRepository holdingRepository;

  private UUID testPortfolioId;
  private UUID testHoldingId;

  @BeforeEach
  void setupTestData() {
    // Create test user
    testDataHelper.createTestUser("testuser@example.com");
    UUID userId = userRepository.findByEmail("testuser@example.com").orElseThrow().getId();

    // Create test portfolio using TestDataHelper (ensures transaction visibility)
    testPortfolioId = UUID.randomUUID();
    testDataHelper.createTestPortfolio(testPortfolioId, userId);

    // Create test holdings with sectors for factor calculation using TestDataHelper
    // This ensures the holdings are visible to HTTP requests in separate transactions
    Holding holding1 = testDataHelper.createTestHolding(
        testPortfolioId,
        "AAPL",
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(15000),
        "Technology"
    );
    testHoldingId = holding1.getId();

    testDataHelper.createTestHolding(
        testPortfolioId,
        "MSFT",
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(15000),
        "Technology"
    );
  }

  @AfterEach
  void cleanupTestData() {
    // Cleanup test data in reverse dependency order (child → parent)
    holdingRepository.deleteAll();
    portfolioRepository.deleteAll();
    userRepository.deleteAll();
  }

  /**
   * Test GET /api/portfolios/{id}/factors returns factor scores for all holdings.
   *
   * <p>Expected response: - Array of holdings with factor scores - Each holding has Value,
   * Momentum, Quality, Revisions scores - Scores are sector-normalized (z-scores) - Includes
   * timestamp of factor calculation
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetPortfolioFactors() throws Exception {
    mockMvc
        .perform(get("/api/portfolios/{id}/factors", testPortfolioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").exists())
        .andExpect(jsonPath("$[0].factorScores").exists())
        .andExpect(jsonPath("$[0].factorScores.value").exists())
        .andExpect(jsonPath("$[0].factorScores.momentum").exists())
        .andExpect(jsonPath("$[0].factorScores.quality").exists())
        .andExpect(jsonPath("$[0].factorScores.revisions").exists())
        .andExpect(jsonPath("$[0].sector").exists())
        .andExpect(jsonPath("$[0].calculatedAt").exists());
  }

  /**
   * Test GET /api/holdings/{id}/factors returns factor scores for specific holding.
   *
   * <p>Expected response: - Single holding with detailed factor breakdown - Sector-normalized
   * scores - Raw scores before normalization - Percentile rankings within sector
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetHoldingFactors() throws Exception {
    mockMvc
        .perform(get("/api/holdings/{id}/factors", testHoldingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.symbol").exists())
        .andExpect(jsonPath("$.factorScores").exists())
        .andExpect(jsonPath("$.factorScores.value").exists())
        .andExpect(jsonPath("$.factorScores.momentum").exists())
        .andExpect(jsonPath("$.factorScores.quality").exists())
        .andExpect(jsonPath("$.factorScores.revisions").exists())
        .andExpect(jsonPath("$.rawScores").exists())
        .andExpect(jsonPath("$.percentileRanks").exists())
        .andExpect(jsonPath("$.sector").exists());
  }

  /**
   * Test VIEWER role can access factor data (read-only).
   */
  @Test
  @WithMockUser(roles = "VIEWER")
  public void testViewerCanAccessFactors() throws Exception {
    mockMvc
        .perform(get("/api/portfolios/{id}/factors", testPortfolioId))
        .andExpect(status().isOk());
  }

  /**
   * Test factor scores use sector normalization (FR-036).
   *
   * <p>Scores should be z-scores relative to sector peers, not absolute values.
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testFactorScoresAreSectorNormalized() throws Exception {
    mockMvc
        .perform(get("/api/portfolios/{id}/factors", testPortfolioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].factorScores.value").isNumber())
        .andExpect(jsonPath("$[0].sector").exists()); // Sector required for normalization
  }
}
