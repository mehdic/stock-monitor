package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
    UUID portfolioId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/portfolios/{id}/factors", portfolioId))
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
    UUID holdingId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/holdings/{id}/factors", holdingId))
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
    UUID portfolioId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/portfolios/{id}/factors", portfolioId))
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
    UUID portfolioId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/portfolios/{id}/factors", portfolioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].factorScores.value").isNumber())
        .andExpect(jsonPath("$[0].sector").exists()); // Sector required for normalization
  }
}
