package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract tests for Constraint API endpoints.
 *
 * <p>Tests for User Story 3 (T132, T133) - Constraint modification and preview
 */
public class ConstraintContractTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private RecommendationRepository recommendationRepository;
  @Autowired private RecommendationRunRepository recommendationRunRepository;
  @Autowired private HoldingRepository holdingRepository;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private ConstraintSetRepository constraintSetRepository;

  private UUID testPortfolioId;
  private UUID testUserId;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    // Ensure test universes exist
    testDataHelper.createTestUniverse("S&P 500");
    testDataHelper.createTestUniverse("Russell 2000");
    testDataHelper.createTestUniverse("Custom Test Universe");

    // Create test portfolio and user for constraint tests
    testUserId = testDataHelper.createTestUser("testuser@example.com").getId();
    testPortfolioId = UUID.randomUUID();
    testDataHelper.createTestPortfolio(testPortfolioId, testUserId);

    // Create viewer user for authorization tests
    testDataHelper.createTestUserWithRole("viewer@example.com",
        com.stockmonitor.model.User.UserRole.VIEWER);

    // Seed data for preview tests (RecommendationRun and Holdings)
    seedPreviewTestData();
  }

  @AfterEach
  void cleanupTestData() {
    // Delete in reverse dependency order (child → parent)
    recommendationRepository.deleteAll();
    recommendationRunRepository.deleteAll();
    holdingRepository.deleteAll();
    constraintSetRepository.deleteAll();
    portfolioRepository.deleteAll();
    userRepository.deleteAll();
  }

  private void seedPreviewTestData() {
    // Create holdings for turnover calculation
    testDataHelper.createTestHolding(testPortfolioId, "AAPL",
        java.math.BigDecimal.valueOf(100), java.math.BigDecimal.valueOf(150.00), "Technology");
    testDataHelper.createTestHolding(testPortfolioId, "MSFT",
        java.math.BigDecimal.valueOf(50), java.math.BigDecimal.valueOf(300.00), "Technology");

    // Create historical recommendation run for preview service
    com.stockmonitor.model.RecommendationRun testRun =
        testDataHelper.createTestRecommendationRun(testUserId, testPortfolioId);

    // Create some recommendations for the run
    testDataHelper.createTestRecommendation(testRun.getId(), "AAPL", 1, "Technology");
    testDataHelper.createTestRecommendation(testRun.getId(), "MSFT", 2, "Technology");
    testDataHelper.createTestRecommendation(testRun.getId(), "GOOGL", 3, "Technology");
    testDataHelper.createTestRecommendation(testRun.getId(), "NVDA", 4, "Technology");
    testDataHelper.createTestRecommendation(testRun.getId(), "META", 5, "Technology");
  }

  /**
   * T132: Contract test for POST /api/portfolios/{id}/constraints/preview
   *
   * <p>Verifies preview endpoint returns impact estimates with expected structure
   */
  @Test
  @DisplayName("POST /api/portfolios/{id}/constraints/preview should return preview DTO")
  public void testConstraintPreview() throws Exception {
    UUID portfolioId = testPortfolioId;

    // Create modified constraints
    ConstraintSetDTO modifiedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(8.0))
            .maxSectorExposurePct(BigDecimal.valueOf(25.0))
            .turnoverCapPct(BigDecimal.valueOf(20.0)) // Changed from 25%
            .build();

    mockMvc
        .perform(
            post("/api/portfolios/{id}/constraints/preview", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedConstraints))
                .header("Authorization", "Bearer " + getValidJwtToken()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.expectedPickCount").exists())
        .andExpect(jsonPath("$.expectedPickCountRange").exists())
        .andExpect(jsonPath("$.expectedTurnoverPct").exists())
        .andExpect(jsonPath("$.expectedTurnoverRange").exists())
        .andExpect(jsonPath("$.affectedPositionsCount").exists())
        .andExpect(jsonPath("$.droppedSymbols").exists())
        .andExpect(jsonPath("$.addedSymbols").exists())
        .andExpect(jsonPath("$.accuracyNote").exists());
  }

  /**
   * T133: Contract test for PUT /api/portfolios/{id}/constraints
   *
   * <p>Verifies constraint save endpoint returns updated constraints
   */
  @Test
  @DisplayName("PUT /api/portfolios/{id}/constraints should save and return constraints")
  public void testSaveConstraints() throws Exception {
    UUID portfolioId = testPortfolioId;

    ConstraintSetDTO updatedConstraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(7.0))
            .maxSectorExposurePct(BigDecimal.valueOf(22.0))
            .turnoverCapPct(BigDecimal.valueOf(18.0))
            .build();

    mockMvc
        .perform(
            put("/api/portfolios/{id}/constraints", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedConstraints))
                .header("Authorization", "Bearer " + getValidJwtToken()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.maxNameWeightLargeCapPct").value(7.0))
        .andExpect(jsonPath("$.maxSectorExposurePct").value(22.0))
        .andExpect(jsonPath("$.turnoverCapPct").value(18.0))
        .andExpect(jsonPath("$.version").exists())
        .andExpect(jsonPath("$.createdAt").exists());
  }

  /**
   * Test that VIEWER role cannot modify constraints (FR-062, FR-063)
   */
  @Test
  @DisplayName("PUT /api/portfolios/{id}/constraints should reject VIEWER role with 403")
  public void testConstraintSaveRejectsViewer() throws Exception {
    UUID portfolioId = testPortfolioId;

    ConstraintSetDTO constraints =
        ConstraintSetDTO.builder()
            .maxNameWeightLargeCapPct(BigDecimal.valueOf(8.0))
            .maxSectorExposurePct(BigDecimal.valueOf(25.0))
            .turnoverCapPct(BigDecimal.valueOf(20.0))
            .build();

    mockMvc
        .perform(
            put("/api/portfolios/{id}/constraints", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(constraints))
                .header("Authorization", "Bearer " + getViewerJwtToken()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Only portfolio owner can modify constraints"));
  }

  /**
   * Test constraint reset endpoint (FR-018)
   */
  @Test
  @DisplayName("POST /api/portfolios/{id}/constraints/reset should restore defaults")
  public void testResetConstraints() throws Exception {
    UUID portfolioId = testPortfolioId;

    mockMvc
        .perform(
            post("/api/portfolios/{id}/constraints/reset", portfolioId)
                .header("Authorization", "Bearer " + getValidJwtToken()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.maxNameWeightLargeCapPct").value(10.0)) // Default value
        .andExpect(jsonPath("$.maxSectorExposurePct").value(30.0)) // Default value
        .andExpect(jsonPath("$.turnoverCapPct").value(25.0)); // Default value
  }

  /**
   * Test constraint validation (T141)
   */
  @Test
  @DisplayName("PUT /api/portfolios/{id}/constraints should validate ranges")
  public void testConstraintValidation() throws Exception {
    UUID portfolioId = testPortfolioId;

    // Invalid: maxNameWeightLargeCapPct > 100%
    ConstraintSetDTO invalidConstraints =
        ConstraintSetDTO.builder().maxNameWeightLargeCapPct(BigDecimal.valueOf(150.0)).build();

    mockMvc
        .perform(
            put("/api/portfolios/{id}/constraints", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidConstraints))
                .header("Authorization", "Bearer " + getValidJwtToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.maxNameWeightLargeCapPct").exists());
  }

  /**
   * Helper to get valid JWT token for OWNER role
   */
  private String getValidJwtToken() {
    return generateTestToken("testuser@example.com");
  }

  /**
   * Helper to get JWT token for VIEWER role
   */
  private String getViewerJwtToken() {
    return generateTestToken("viewer@example.com");
  }
}
