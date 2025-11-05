package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.ConstraintSetDTO;
import java.math.BigDecimal;
import java.util.*;
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

  /**
   * T132: Contract test for POST /api/portfolios/{id}/constraints/preview
   *
   * <p>Verifies preview endpoint returns impact estimates with expected structure
   */
  @Test
  @DisplayName("POST /api/portfolios/{id}/constraints/preview should return preview DTO")
  public void testConstraintPreview() throws Exception {
    UUID portfolioId = UUID.randomUUID();

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
    UUID portfolioId = UUID.randomUUID();

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
    UUID portfolioId = UUID.randomUUID();

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
    UUID portfolioId = UUID.randomUUID();

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
    UUID portfolioId = UUID.randomUUID();

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
