package com.stockmonitor.controller;

import com.stockmonitor.dto.HoldingsUploadResponse;
import com.stockmonitor.dto.PerformanceMetricsDTO;
import com.stockmonitor.dto.PortfolioDTO;
import com.stockmonitor.model.Holding;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.PerformanceAttributionService;
import com.stockmonitor.service.PortfolioService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portfolio controller handling portfolio and holdings management.
 *
 * <p>Endpoints: - GET /api/portfolios/{id} - Get portfolio details - POST
 * /api/portfolios/{id}/holdings/upload - Upload holdings CSV - GET /api/portfolios/{id}/holdings -
 * Get all holdings - GET /api/portfolios/{id}/performance - Get performance metrics (T158)
 */
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final PerformanceAttributionService performanceAttributionService;
  private final UserRepository userRepository;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PortfolioDTO> createPortfolio() {
    // Extract user ID from authentication context (don't trust request body)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = authentication.getName();
    UUID userId = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new IllegalStateException("User not found"))
        .getId();

    log.info("Create portfolio request for user: {}", userId);
    PortfolioDTO portfolio = portfolioService.getOrCreatePortfolio(userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PortfolioDTO> getPortfolio(@PathVariable("id") UUID id) {
    log.info("Get portfolio request for ID: {}", id);
    PortfolioDTO portfolio = portfolioService.getPortfolio(id);
    return ResponseEntity.ok(portfolio);
  }

  @PostMapping(value = "/{id}/holdings/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<HoldingsUploadResponse> uploadHoldings(
      @PathVariable("id") UUID id, @RequestParam("file") MultipartFile file) throws Exception {
    log.info("Upload holdings request for portfolio: {}, file: {}", id, file.getOriginalFilename());

    HoldingsUploadResponse response = portfolioService.uploadHoldings(id, file.getInputStream());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/holdings")
  public ResponseEntity<List<Holding>> getHoldings(@PathVariable("id") UUID id) {
    log.info("Get holdings request for portfolio: {}", id);
    List<Holding> holdings = portfolioService.getHoldings(id);
    return ResponseEntity.ok(holdings);
  }

  /**
   * Get performance metrics for portfolio (T158, FR-008, FR-014).
   *
   * @param id Portfolio UUID
   * @param startDate Period start date (optional, defaults to last month)
   * @param endDate Period end date (optional, defaults to today)
   * @return Performance metrics with P&L and contributors/detractors
   */
  @GetMapping("/{id}/performance")
  @PreAuthorize("hasRole('OWNER') or hasRole('VIEWER')")
  public ResponseEntity<PerformanceMetricsDTO> getPerformance(
      @PathVariable("id") UUID id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    log.info("Get performance for portfolio: {} from {} to {}", id, startDate, endDate);
    PerformanceMetricsDTO performance =
        performanceAttributionService.calculatePerformance(id, startDate, endDate);
    return ResponseEntity.ok(performance);
  }
}
