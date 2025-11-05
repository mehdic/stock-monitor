package com.stockmonitor.service;

import com.stockmonitor.dto.ExclusionDTO;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating human-readable exclusion explanations (T194, FR-031).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExclusionReasonService {

  public List<ExclusionDTO> getExclusionsForRun(UUID runId) {
    // TODO: Query exclusions from database
    return List.of();
  }

  public String generateExplanation(String reasonCode, Object... params) {
    return switch (reasonCode) {
      case ExclusionDTO.ReasonCode.LIQUIDITY_FLOOR ->
          "Liquidity tier below minimum threshold";
      case ExclusionDTO.ReasonCode.SECTOR_CAP ->
          "Sector exposure would exceed cap";
      case ExclusionDTO.ReasonCode.EARNINGS_PROXIMITY ->
          "Earnings announcement within blackout period";
      case ExclusionDTO.ReasonCode.SPREAD_THRESHOLD ->
          "Bid-ask spread exceeds threshold";
      case ExclusionDTO.ReasonCode.MARKET_CAP_FLOOR ->
          "Market cap below minimum";
      case ExclusionDTO.ReasonCode.POSITION_SIZE_CAP ->
          "Position size would exceed maximum";
      default -> "Stock excluded";
    };
  }
}
