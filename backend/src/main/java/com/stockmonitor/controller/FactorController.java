package com.stockmonitor.controller;

import com.stockmonitor.dto.FactorScoreDTO;
import com.stockmonitor.dto.HoldingFactorResponse;
import com.stockmonitor.service.FactorService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for factor analysis endpoints (T154).
 *
 * <p>Endpoints: - GET /api/portfolios/{id}/factors - Get factor scores for all holdings - GET
 * /api/holdings/{id}/factors - Get factor scores for specific holding
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class FactorController {

  private final FactorService factorService;

  /**
   * Get factor scores for all holdings in portfolio (FR-034, FR-036).
   *
   * @param portfolioId Portfolio UUID
   * @return List of factor scores with sector normalization
   */
  @GetMapping("/api/portfolios/{portfolioId}/factors")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<List<HoldingFactorResponse>> getPortfolioFactors(@PathVariable UUID portfolioId) {
    log.info("Get factors for portfolio: {}", portfolioId);
    List<FactorScoreDTO> factors = factorService.getPortfolioFactors(portfolioId);
    List<HoldingFactorResponse> responses = factors.stream()
        .map(HoldingFactorResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  /**
   * Get factor scores for specific holding with detailed breakdown (FR-034, FR-036).
   *
   * @param holdingId Holding UUID
   * @return Factor scores with raw scores and percentiles
   */
  @GetMapping("/api/holdings/{holdingId}/factors")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<HoldingFactorResponse> getHoldingFactors(@PathVariable UUID holdingId) {
    log.info("Get factors for holding: {}", holdingId);
    FactorScoreDTO factors = factorService.getHoldingFactors(holdingId);
    HoldingFactorResponse response = HoldingFactorResponse.from(factors);
    return ResponseEntity.ok(response);
  }
}
