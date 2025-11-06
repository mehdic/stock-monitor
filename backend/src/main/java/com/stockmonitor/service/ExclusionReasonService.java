package com.stockmonitor.service;

import com.stockmonitor.dto.ExclusionDTO;
import com.stockmonitor.model.Exclusion;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.ExclusionRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

  private final ExclusionRepository exclusionRepository;
  private final RecommendationRunRepository recommendationRunRepository;

  public List<ExclusionDTO> getExclusionsForRun(UUID runId) {
    log.debug("Fetching exclusions for run: {}", runId);

    // Get the run to find the run date
    RecommendationRun run = recommendationRunRepository.findById(runId).orElse(null);

    List<Exclusion> exclusions = exclusionRepository.findByRunId(runId);

    return exclusions.stream()
        .map(exclusion -> ExclusionDTO.builder()
            .symbol(exclusion.getSymbol())
            .companyName(exclusion.getSymbol()) // Company name not stored in Exclusion, using symbol for now
            .exclusionReasonCode(exclusion.getExclusionReasonCode())
            .explanation(generateExplanation(exclusion.getExclusionReasonCode()))
            .runDate(run != null ? run.getScheduledDate() : null)
            .build())
        .collect(Collectors.toList());
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
