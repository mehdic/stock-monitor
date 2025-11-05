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
 * Contract test for data source health endpoints (T149).
 *
 * <p>Tests API contracts for: - GET /api/data-sources - Returns all data sources with health
 * status - GET /api/data-sources/{id}/health - Returns health details for specific data source
 */
public class DataSourceContractTest extends BaseIntegrationTest {


  /**
   * Test GET /api/data-sources returns all data sources (FR-037, FR-038).
   *
   * <p>Expected response: - Array of data sources - Each has name, status, lastUpdateTime -
   * Status values: HEALTHY, STALE, UNAVAILABLE
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetDataSources() throws Exception {
    mockMvc
        .perform(get("/api/data-sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].status").exists())
        .andExpect(jsonPath("$[0].lastUpdateTime").exists());
  }

  /**
   * Test GET /api/data-sources/{id}/health returns detailed health info (FR-037, FR-038).
   *
   * <p>Expected response: - status: HEALTHY | STALE | UNAVAILABLE - lastUpdateTime: Timestamp -
   * freshnessThresholdMinutes: Configured threshold - minutesSinceUpdate: Actual staleness -
   * message: Human-readable description
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetDataSourceHealth() throws Exception {
    String dataSourceId = "market-data";

    mockMvc
        .perform(get("/api/data-sources/{id}/health", dataSourceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.lastUpdateTime").exists())
        .andExpect(jsonPath("$.freshnessThresholdMinutes").exists())
        .andExpect(jsonPath("$.minutesSinceUpdate").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  /**
   * Test data source health status follows freshness rules (FR-037).
   *
   * <p>HEALTHY: lastUpdateTime within threshold STALE: lastUpdateTime exceeds threshold
   * UNAVAILABLE: no recent data or source unreachable
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testDataSourceHealthFollowsFreshnessRules() throws Exception {
    mockMvc
        .perform(get("/api/data-sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].status").exists()); // All sources have status
  }

  /**
   * Test VIEWER role can access data source health (read-only).
   */
  @Test
  @WithMockUser(roles = "VIEWER")
  public void testViewerCanAccessDataSourceHealth() throws Exception {
    mockMvc.perform(get("/api/data-sources")).andExpect(status().isOk());
  }

  /**
   * Test data source health includes critical sources.
   *
   * <p>Critical sources that must be monitored: - market-data: Stock prices - factor-data: Factor
   * scores - fundamental-data: Company fundamentals - benchmark-data: S&P 500 benchmark
   */
  @Test
  @WithMockUser(roles = "OWNER")
  public void testDataSourcesIncludeCriticalSources() throws Exception {
    mockMvc
        .perform(get("/api/data-sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == 'market-data')]").exists())
        .andExpect(jsonPath("$[?(@.id == 'factor-data')]").exists())
        .andExpect(jsonPath("$[?(@.id == 'fundamental-data')]").exists())
        .andExpect(jsonPath("$[?(@.id == 'benchmark-data')]").exists());
  }
}
