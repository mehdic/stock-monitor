package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.repository.RecommendationRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import com.stockmonitor.repository.PortfolioRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Contract tests for Recommendation API endpoints.
 *
 * <p>Tests verify API contracts per specs/001-month-end-analyst/contracts/rest-api.yaml
 *
 * <p>T040: POST /api/runs T041: GET /api/runs/{id}/recommendations
 */
class RecommendationContractTest extends BaseIntegrationTest {

  @Autowired private ObjectMapper objectMapper;
  @Autowired private RecommendationRepository recommendationRepository;
  @Autowired private RecommendationRunRepository recommendationRunRepository;
  @Autowired private PortfolioRepository portfolioRepository;

  private String authToken;
  private String portfolioId;
  private java.util.UUID userId;

  @BeforeEach
  void setUpAuth() {
    // Ensure test universes exist
    testDataHelper.createTestUniverse("S&P 500");
    testDataHelper.createTestUniverse("Russell 2000");
    testDataHelper.createTestUniverse("Custom Test Universe");

    authToken = generateTestToken("testuser@example.com");
    portfolioId = "00000000-0000-0000-0000-000000000001";

    // Get the created user
    userId = userRepository.findByEmail("testuser@example.com").orElseThrow().getId();

    // Create test portfolio
    testDataHelper.createTestPortfolio(java.util.UUID.fromString(portfolioId), userId);

    // Create test recommendation runs with specific IDs for tests
    // Run 1: Completed run with recommendations
    testDataHelper.createTestRecommendationRunWithId(
        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
        userId,
        java.util.UUID.fromString(portfolioId)
    );

    // Run 2: In-progress run with no recommendations yet
    testDataHelper.createTestRecommendationRunInProgress(
        java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
        userId,
        java.util.UUID.fromString(portfolioId)
    );
  }

  @AfterEach
  void cleanupTestData() {
    // Delete in reverse dependency order (child → parent)
    recommendationRepository.deleteAll();
    recommendationRunRepository.deleteAll();
    portfolioRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void testTriggerRecommendationRun_Success() throws Exception {
    // Given
    Map<String, Object> runRequest = new HashMap<>();
    runRequest.put("portfolioId", portfolioId);
    runRequest.put("runType", "OFF_CYCLE");

    // When & Then
    mockMvc
        .perform(
            post("/api/runs")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.runType").value("OFF_CYCLE"))
        .andExpect(jsonPath("$.portfolioId").value(portfolioId));
  }

  @Test
  void testTriggerRecommendationRun_DataNotFresh() throws Exception {
    // Given - simulate stale data scenario
    Map<String, Object> runRequest = new HashMap<>();
    runRequest.put("portfolioId", portfolioId);
    runRequest.put("runType", "OFF_CYCLE");

    // When & Then - assuming data freshness check fails
    mockMvc
        .perform(
            post("/api/runs")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest)))
        .andExpect(status().isAccepted()) // Still accepts but marks data freshness issue
        .andExpect(jsonPath("$.dataFreshnessCheckPassed").value(false))
        .andExpect(jsonPath("$.dataFreshnessSnapshot").exists());
  }

  @Test
  void testTriggerRecommendationRun_MissingPortfolio() throws Exception {
    // Given
    Map<String, Object> runRequest = new HashMap<>();
    runRequest.put("portfolioId", "99999999-9999-9999-9999-999999999999");
    runRequest.put("runType", "ADHOC");

    // When & Then
    mockMvc
        .perform(
            post("/api/runs")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Portfolio not found"));
  }

  @Test
  void testTriggerRecommendationRun_Unauthorized() throws Exception {
    // Given
    Map<String, Object> runRequest = new HashMap<>();
    runRequest.put("portfolioId", portfolioId);

    // When & Then - no auth token
    mockMvc
        .perform(
            post("/api/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetRecommendations_Success() throws Exception {
    // Given - assume a run exists
    String runId = "00000000-0000-0000-0000-000000000001";

    // When & Then
    mockMvc
        .perform(
            get("/api/runs/" + runId + "/recommendations")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].ticker").exists())
        .andExpect(jsonPath("$[0].action").exists())
        .andExpect(jsonPath("$[0].confidenceScore").exists())
        .andExpect(jsonPath("$[0].explanation").exists())
        .andExpect(jsonPath("$[0].factorDrivers").exists())
        .andExpect(jsonPath("$[0].rank").exists());
  }

  @Test
  void testGetRecommendations_Ranked() throws Exception {
    // Given - assume a run exists with multiple recommendations
    String runId = "00000000-0000-0000-0000-000000000001";

    // When & Then - verify recommendations are ranked
    mockMvc
        .perform(
            get("/api/runs/" + runId + "/recommendations")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[1].rank").value(2));
  }

  @Test
  void testGetRecommendations_WithDriverExplanations() throws Exception {
    // Given
    String runId = "00000000-0000-0000-0000-000000000001";

    // When & Then - verify factor drivers are included
    mockMvc
        .perform(
            get("/api/runs/" + runId + "/recommendations")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].factorDrivers").isMap())
        .andExpect(jsonPath("$[0].explanation").isString());
  }

  @Test
  void testGetRecommendations_RunNotFound() throws Exception {
    // Given
    String nonExistentRunId = "99999999-9999-9999-9999-999999999999";

    // When & Then
    mockMvc
        .perform(
            get("/api/runs/" + nonExistentRunId + "/recommendations")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Recommendation run not found"));
  }

  @Test
  void testGetRecommendations_RunNotCompleted() throws Exception {
    // Given - assume a run that's still in progress
    String inProgressRunId = "00000000-0000-0000-0000-000000000002";

    // When & Then
    mockMvc
        .perform(
            get("/api/runs/" + inProgressRunId + "/recommendations")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0)); // No recommendations yet
  }

  @Test
  void testGetRunStatus_Success() throws Exception {
    // Given
    String runId = "00000000-0000-0000-0000-000000000001";

    // When & Then
    mockMvc
        .perform(get("/api/runs/" + runId).header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(runId))
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.dataFreshnessCheckPassed").exists())
        .andExpect(jsonPath("$.numRecommendations").exists());
  }
}
