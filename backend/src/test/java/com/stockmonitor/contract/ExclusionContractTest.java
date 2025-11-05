package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
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

  @Test
  @WithMockUser(roles = "OWNER")
  public void testGetExclusions() throws Exception {
    UUID runId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/runs/{id}/exclusions", runId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].symbol").exists())
        .andExpect(jsonPath("$[0].exclusionReasonCode").exists())
        .andExpect(jsonPath("$[0].explanation").exists());
  }

  @Test
  @WithMockUser(roles = "OWNER")
  public void testExportExclusions() throws Exception {
    UUID runId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/runs/{id}/exclusions/export", runId))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv"))
        .andExpect(header().exists("Content-Disposition"));
  }
}
