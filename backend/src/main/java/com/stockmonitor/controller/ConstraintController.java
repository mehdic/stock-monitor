package com.stockmonitor.controller;

import com.stockmonitor.dto.ConstraintPreviewDTO;
import com.stockmonitor.dto.ConstraintSetDTO;
import com.stockmonitor.model.ConstraintSet;
import com.stockmonitor.service.ConstraintPreviewService;
import com.stockmonitor.service.ConstraintService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Constraint controller handling portfolio constraints.
 *
 * <p>Endpoints: - GET /api/constraints/defaults - Get default constraints - GET
 * /api/portfolios/{id}/constraints - Get constraints for portfolio - PUT
 * /api/portfolios/{id}/constraints - Update constraints (OWNER only) - POST
 * /api/portfolios/{id}/constraints/reset - Reset to defaults (OWNER only)
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ConstraintController {

  private final ConstraintService constraintService;
  private final ConstraintPreviewService constraintPreviewService;

  @GetMapping("/api/constraints/defaults")
  public ResponseEntity<ConstraintSetDTO> getDefaults() {
    log.info("Get default constraints request");
    ConstraintSetDTO defaults = constraintService.getDefaultConstraints();
    return ResponseEntity.ok(defaults);
  }

  @GetMapping("/api/portfolios/{id}/constraints")
  public ResponseEntity<ConstraintSetDTO> getConstraintsForPortfolio(@PathVariable UUID id) {
    log.info("Get constraints for portfolio: {}", id);
    ConstraintSetDTO constraints = constraintService.getConstraintsForPortfolio(id);
    return ResponseEntity.ok(constraints);
  }

  /**
   * Preview impact of constraint changes (T138 - FR-017).
   *
   * <p>Simulates optimizer with new constraints using last run's factor scores. Returns estimates
   * with accuracy ranges (±10% picks, ±15% turnover).
   */
  @PostMapping("/api/portfolios/{portfolioId}/constraints/preview")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<ConstraintPreviewDTO> previewConstraintImpact(
      @PathVariable UUID portfolioId, @RequestBody ConstraintSetDTO modifiedConstraints) {
    log.info("Preview constraint impact for portfolio: {}", portfolioId);

    try {
      ConstraintPreviewDTO preview =
          constraintPreviewService.previewConstraintImpact(portfolioId, modifiedConstraints);
      return ResponseEntity.ok(preview);
    } catch (IllegalStateException e) {
      // No historical data available
      log.warn("Cannot preview constraints for portfolio {}: {}", portfolioId, e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Update constraints for a portfolio (T140 - FR-016, FR-019).
   *
   * <p>Creates new constraint set with incremented version. Previous versions retained for audit.
   */
  @PutMapping("/api/portfolios/{portfolioId}/constraints")
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<ConstraintSetDTO> updateConstraints(
      @PathVariable UUID portfolioId, @RequestBody ConstraintSetDTO dto) {
    log.info("Update constraints for portfolio: {}", portfolioId);

    ConstraintSet updated = constraintService.updateConstraints(portfolioId, dto);
    return ResponseEntity.ok(ConstraintSetDTO.from(updated));
  }

  /**
   * Reset constraints to defaults (T140 - FR-018).
   *
   * <p>Creates new constraint set with default values and incremented version.
   */
  @PostMapping("/api/portfolios/{portfolioId}/constraints/reset")
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<ConstraintSetDTO> resetToDefaults(@PathVariable UUID portfolioId) {
    log.info("Reset constraints to defaults for portfolio: {}", portfolioId);

    ConstraintSet defaults = constraintService.resetPortfolioConstraintsToDefaults(portfolioId);
    return ResponseEntity.ok(ConstraintSetDTO.from(defaults));
  }
}
