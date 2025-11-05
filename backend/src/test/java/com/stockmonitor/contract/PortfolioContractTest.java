package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.helper.TestDataHelper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Contract tests for Portfolio API endpoints.
 *
 * <p>Tests verify API contracts per specs/001-month-end-analyst/contracts/rest-api.yaml
 *
 * <p>T038: POST /api/portfolios/{id}/holdings/upload
 */
class PortfolioContractTest extends BaseIntegrationTest {

  @Autowired
  private TestDataHelper testDataHelper;

  private String authToken;
  private String portfolioId;
  private TestDataHelper.TestDataContext testContext;

  @BeforeEach
  void setUpAuth() throws Exception {
    // Generate a valid JWT token for testing
    authToken = generateTestToken("testuser@example.com");

    // Create complete test data setup (portfolio, universe, constraints)
    UUID testUserId = UUID.randomUUID();
    testContext = testDataHelper.createCompleteTestSetup(testUserId);
    portfolioId = testContext.getPortfolio().getId().toString();
  }

  @Test
  void testUploadHoldings_Success() throws Exception {
    // Given
    String csvContent =
        "ticker,quantity,cost_basis,currency\n"
            + "AAPL,100,150.50,USD\n"
            + "MSFT,50,300.00,USD\n"
            + "GOOGL,25,2500.00,USD";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "holdings.csv", "text/csv", csvContent.getBytes());

    // When & Then
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/portfolios/" + portfolioId + "/holdings/upload")
                .file(file)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHoldings").value(3))
        .andExpect(jsonPath("$.marketValue").exists())
        .andExpect(jsonPath("$.uploadedAt").exists());
  }

  @Test
  void testUploadHoldings_InvalidCsv() throws Exception {
    // Given - CSV with invalid data
    String csvContent =
        "ticker,quantity,cost_basis,currency\n"
            + "AAPL,invalid,150.50,USD\n" // invalid quantity
            + "MSFT,-50,300.00,USD"; // negative quantity

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "holdings.csv", "text/csv", csvContent.getBytes());

    // When & Then
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/portfolios/" + portfolioId + "/holdings/upload")
                .file(file)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors").exists())
        .andExpect(jsonPath("$.validationErrors[0].row").exists())
        .andExpect(jsonPath("$.validationErrors[0].errorCode").exists());
  }

  @Test
  void testUploadHoldings_MissingRequiredColumns() throws Exception {
    // Given - CSV missing required columns
    String csvContent = "ticker,quantity\n" + "AAPL,100";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "holdings.csv", "text/csv", csvContent.getBytes());

    // When & Then
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/portfolios/" + portfolioId + "/holdings/upload")
                .file(file)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Missing required columns: cost_basis, currency"));
  }

  @Test
  void testUploadHoldings_EmptyFile() throws Exception {
    // Given - empty CSV
    String csvContent = "";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "holdings.csv", "text/csv", csvContent.getBytes());

    // When & Then
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/portfolios/" + portfolioId + "/holdings/upload")
                .file(file)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("CSV file is empty"));
  }

  @Test
  void testUploadHoldings_Unauthorized() throws Exception {
    // Given
    String csvContent = "ticker,quantity,cost_basis,currency\nAAPL,100,150.50,USD";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "holdings.csv", "text/csv", csvContent.getBytes());

    // When & Then - no auth token
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/portfolios/" + portfolioId + "/holdings/upload")
                .file(file))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGetPortfolio_Success() throws Exception {
    // When & Then
    mockMvc
        .perform(
            get("/api/portfolios/" + portfolioId)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(portfolioId))
        .andExpect(jsonPath("$.cash").exists())
        .andExpect(jsonPath("$.marketValue").exists())
        .andExpect(jsonPath("$.totalValue").exists());
  }

  @Test
  void testGetPortfolio_NotFound() throws Exception {
    // Given
    String nonExistentId = "99999999-9999-9999-9999-999999999999";

    // When & Then
    mockMvc
        .perform(
            get("/api/portfolios/" + nonExistentId)
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Portfolio not found"));
  }

  /**
   * Test GET /api/portfolios/{id}/performance returns performance metrics (T148, FR-008, FR-014).
   *
   * <p>Expected response: - totalPnL: Absolute P&L in dollars - totalPnLPct: P&L as percentage -
   * benchmarkReturn: Benchmark return for same period - excessReturn: Portfolio return -
   * benchmark return - topContributors: Top 5 holdings by P&L contribution - topDetractors: Top 5
   * holdings by P&L drag - periodStart, periodEnd: Date range for metrics
   */
  @Test
  void testGetPortfolioPerformance_Success() throws Exception {
    mockMvc
        .perform(
            get("/api/portfolios/" + portfolioId + "/performance")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPnL").exists())
        .andExpect(jsonPath("$.totalPnLPct").exists())
        .andExpect(jsonPath("$.benchmarkReturn").exists())
        .andExpect(jsonPath("$.excessReturn").exists())
        .andExpect(jsonPath("$.topContributors").isArray())
        .andExpect(jsonPath("$.topDetractors").isArray())
        .andExpect(jsonPath("$.periodStart").exists())
        .andExpect(jsonPath("$.periodEnd").exists());
  }

  /**
   * Test performance contributors include position details (FR-014).
   *
   * <p>Each contributor/detractor should include: - symbol - pnl: Contribution to total P&L -
   * pnlPct: Contribution as percentage - weight: Position weight in portfolio
   */
  @Test
  void testGetPortfolioPerformance_ContributorsIncludeDetails() throws Exception {
    mockMvc
        .perform(
            get("/api/portfolios/" + portfolioId + "/performance")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topContributors[0].symbol").exists())
        .andExpect(jsonPath("$.topContributors[0].pnl").exists())
        .andExpect(jsonPath("$.topContributors[0].pnlPct").exists())
        .andExpect(jsonPath("$.topContributors[0].weight").exists());
  }

  /**
   * Test performance accepts date range parameters (optional).
   *
   * <p>Query params: ?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD If not provided, defaults to last
   * month.
   */
  @Test
  void testGetPortfolioPerformance_AcceptsDateRange() throws Exception {
    mockMvc
        .perform(
            get("/api/portfolios/" + portfolioId + "/performance")
                .param("startDate", "2024-10-01")
                .param("endDate", "2024-10-31")
                .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodStart").value("2024-10-01"))
        .andExpect(jsonPath("$.periodEnd").value("2024-10-31"));
  }
}
