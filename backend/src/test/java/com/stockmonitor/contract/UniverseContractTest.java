package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.model.Universe;
import com.stockmonitor.model.UniverseConstituent;
import com.stockmonitor.repository.UniverseConstituentRepository;
import com.stockmonitor.repository.UniverseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

/**
 * Contract tests for Universe API endpoints.
 *
 * <p>Tests verify API contracts per specs/001-month-end-analyst/contracts/rest-api.yaml
 *
 * <p>T039: GET /api/universes
 */
class UniverseContractTest extends BaseIntegrationTest {

  private String authToken;

  @Autowired
  private UniverseRepository universeRepository;

  @Autowired
  private UniverseConstituentRepository universeConstituentRepository;

  @BeforeEach
  void setUpAuth() {
    authToken = generateTestToken("testuser@example.com");

    // Seed default universes if not already present
    if (universeRepository.count() == 0) {
      Universe sp500 = Universe.builder()
          .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
          .name("S&P 500")
          .description("S&P 500 Index constituents")
          .type("INDEX")
          .benchmarkSymbol("SPY")
          .constituentCount(500)
          .minMarketCap(java.math.BigDecimal.valueOf(10_000_000_000L))
          .effectiveDate(java.time.LocalDate.now())
          .isActive(true)
          .version(1)
          .build();

      Universe sp500MidCap = Universe.builder()
          .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
          .name("S&P 500 + Mid-Caps")
          .description("S&P 500 plus mid-cap stocks")
          .type("INDEX")
          .benchmarkSymbol("SPY")
          .constituentCount(900)
          .minMarketCap(java.math.BigDecimal.valueOf(2_000_000_000L))
          .effectiveDate(java.time.LocalDate.now())
          .isActive(true)
          .version(1)
          .build();

      Universe russell2000 = Universe.builder()
          .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
          .name("Russell 2000")
          .description("Russell 2000 small-cap index")
          .type("INDEX")
          .benchmarkSymbol("IWM")
          .constituentCount(2000)
          .minMarketCap(java.math.BigDecimal.valueOf(300_000_000L))
          .maxMarketCap(java.math.BigDecimal.valueOf(10_000_000_000L))
          .effectiveDate(java.time.LocalDate.now())
          .isActive(true)
          .version(1)
          .build();

      universeRepository.saveAll(Arrays.asList(sp500, sp500MidCap, russell2000));

      // Add some test constituents for each universe
      UUID sp500Id = UUID.fromString("00000000-0000-0000-0000-000000000001");
      UUID sp500MidCapId = UUID.fromString("00000000-0000-0000-0000-000000000002");
      UUID russell2000Id = UUID.fromString("00000000-0000-0000-0000-000000000003");

      // Add constituents for S&P 500
      universeConstituentRepository.saveAll(Arrays.asList(
          createConstituent(sp500Id, "AAPL", "Apple Inc.", "Technology"),
          createConstituent(sp500Id, "MSFT", "Microsoft Corporation", "Technology"),
          createConstituent(sp500Id, "GOOGL", "Alphabet Inc.", "Technology")
      ));

      // Add constituents for S&P 500 + Mid-Caps
      universeConstituentRepository.saveAll(Arrays.asList(
          createConstituent(sp500MidCapId, "AAPL", "Apple Inc.", "Technology"),
          createConstituent(sp500MidCapId, "MSFT", "Microsoft Corporation", "Technology"),
          createConstituent(sp500MidCapId, "GOOGL", "Alphabet Inc.", "Technology"),
          createConstituent(sp500MidCapId, "ZS", "Zscaler Inc.", "Technology")
      ));

      // Add constituents for Russell 2000
      universeConstituentRepository.saveAll(Arrays.asList(
          createConstituent(russell2000Id, "CELH", "Celsius Holdings", "Consumer Staples"),
          createConstituent(russell2000Id, "CHWY", "Chewy Inc.", "Consumer Discretionary")
      ));
    }
  }

  @AfterEach
  void cleanupTestData() {
    // Delete in reverse dependency order (child → parent)
    universeConstituentRepository.deleteAll();
    universeRepository.deleteAll();
    userRepository.deleteAll();
  }

  private UniverseConstituent createConstituent(UUID universeId, String symbol, String companyName, String sector) {
    return UniverseConstituent.builder()
        .universeId(universeId)
        .symbol(symbol)
        .companyName(companyName)
        .sector(sector)
        .industry("Technology Services")
        .marketCapTier("LARGE_CAP")
        .liquidityTier(5)
        .avgDailyVolume(BigDecimal.valueOf(50_000_000))
        .avgDailyValue(BigDecimal.valueOf(5_000_000_000L))
        .isActive(true)
        .addedDate(LocalDate.now())
        .build();
  }

  @Test
  void testGetUniverses_ReturnsPresetUniverses() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/universes").header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3)) // 3 default universes per T012
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].description").exists())
        .andExpect(jsonPath("$[0].tickerList").isArray());
  }

  @Test
  void testGetUniverses_VerifyDefaultUniverses() throws Exception {
    // When & Then - verify specific default universes exist
    mockMvc
        .perform(get("/api/universes").header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'S&P 500')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'S&P 500 + Mid-Caps')]").exists())
        .andExpect(jsonPath("$[?(@.name == 'Russell 2000')]").exists());
  }

  @Test
  void testGetUniverseById_Success() throws Exception {
    // Given - get first universe ID
    String universeId = "00000000-0000-0000-0000-000000000001";

    // When & Then
    mockMvc
        .perform(
            get("/api/universes/" + universeId).header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(universeId))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.tickerList").isArray())
        .andExpect(jsonPath("$.marketCapMin").exists())
        .andExpect(jsonPath("$.marketCapMax").exists())
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  void testGetUniverseById_NotFound() throws Exception {
    // Given
    String nonExistentId = "99999999-9999-9999-9999-999999999999";

    // When & Then
    mockMvc
        .perform(
            get("/api/universes/" + nonExistentId).header("Authorization", "Bearer " + authToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Universe not found"));
  }

  @Test
  void testGetUniverses_Unauthorized() throws Exception {
    // When & Then - no auth token
    mockMvc
        .perform(get("/api/universes"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testSelectUniverseForPortfolio_Success() throws Exception {
    // Given
    String portfolioId = "00000000-0000-0000-0000-000000000001";
    String universeId = "00000000-0000-0000-0000-000000000001";

    // When & Then
    mockMvc
        .perform(
            put("/api/portfolios/" + portfolioId + "/universe")
                .header("Authorization", "Bearer " + authToken)
                .param("universeId", universeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selectedUniverseId").value(universeId))
        .andExpect(jsonPath("$.coveragePercentage").exists());
  }

  @Test
  void testSelectUniverseForPortfolio_InvalidUniverse() throws Exception {
    // Given
    String portfolioId = "00000000-0000-0000-0000-000000000001";
    String invalidUniverseId = "99999999-9999-9999-9999-999999999999";

    // When & Then
    mockMvc
        .perform(
            put("/api/portfolios/" + portfolioId + "/universe")
                .header("Authorization", "Bearer " + authToken)
                .param("universeId", invalidUniverseId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Universe not found"));
  }
}
