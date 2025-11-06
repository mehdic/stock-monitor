package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.stockmonitor.dto.ExclusionDTO;
import com.stockmonitor.model.Exclusion;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.ExclusionRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for exclusions endpoint (T190, FR-031, FR-032).
 */
public class ExclusionContractTest extends BaseIntegrationTest {

  @Autowired
  private RecommendationRunRepository recommendationRunRepository;

  @Autowired
  private ExclusionRepository exclusionRepository;

  private UUID testRunId;

  @BeforeEach
  void setupTestData() {
    // Create a test recommendation run
    testRunId = UUID.randomUUID();
    RecommendationRun run = RecommendationRun.builder()
        .id(testRunId)
        .userId(UUID.randomUUID())
        .universeId(UUID.randomUUID())
        .portfolioId(UUID.randomUUID())
        .constraintSetId(UUID.randomUUID())
        .runType("MONTH_END")
        .status("COMPLETED")
        .scheduledDate(LocalDate.now())
        .recommendationCount(10)
        .exclusionCount(2)
        .build();
    recommendationRunRepository.save(run);

    // Create test exclusions
    Exclusion exclusion1 = Exclusion.builder()
        .runId(testRunId)
        .symbol("AAPL")
        .exclusionReasonCode(ExclusionDTO.ReasonCode.LIQUIDITY_FLOOR)
        .exclusionReasonText("Liquidity tier below minimum threshold")
        .sector("Technology")
        .marketCapTier("LARGE_CAP")
        .liquidityTier(1)
        .currentPrice(BigDecimal.valueOf(150.00))
        .build();

    Exclusion exclusion2 = Exclusion.builder()
        .runId(testRunId)
        .symbol("TSLA")
        .exclusionReasonCode(ExclusionDTO.ReasonCode.SECTOR_CAP)
        .exclusionReasonText("Sector exposure would exceed cap")
        .sector("Consumer Discretionary")
        .marketCapTier("LARGE_CAP")
        .liquidityTier(3)
        .currentPrice(BigDecimal.valueOf(250.00))
        .build();

    exclusionRepository.save(exclusion1);
    exclusionRepository.save(exclusion2);
  }

  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetExclusions() throws Exception {
    mockMvc
        .perform(get("/api/runs/{id}/exclusions", testRunId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").exists())
        .andExpect(jsonPath("$[0].exclusionReasonCode").exists())
        .andExpect(jsonPath("$[0].explanation").exists());
  }

  @Test
  @WithMockUser(roles = "OWNER")
  public void testExportExclusions() throws Exception {
    mockMvc
        .perform(get("/api/runs/{id}/exclusions/export", testRunId))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv"))
        .andExpect(header().exists("Content-Disposition"));
  }
}
