package com.stockmonitor.dto;

import com.stockmonitor.model.Portfolio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
  private UUID id;
  private UUID userId;
  private BigDecimal cash;
  private BigDecimal marketValue;
  private BigDecimal totalValue;
  private BigDecimal unrealizedPnl;
  private BigDecimal benchmarkReturnPct;
  private BigDecimal relativeReturnPct;
  private BigDecimal universeCoveragePct;
  private LocalDateTime lastCalculatedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static PortfolioDTO from(Portfolio portfolio) {
    BigDecimal totalValue = portfolio.getCashBalance().add(portfolio.getTotalMarketValue());

    return PortfolioDTO.builder()
        .id(portfolio.getId())
        .userId(portfolio.getUserId())
        .cash(portfolio.getCashBalance())
        .marketValue(portfolio.getTotalMarketValue())
        .totalValue(totalValue)
        .unrealizedPnl(portfolio.getUnrealizedPnl())
        .benchmarkReturnPct(portfolio.getBenchmarkReturnPct())
        .relativeReturnPct(portfolio.getRelativeReturnPct())
        .universeCoveragePct(portfolio.getUniverseCoveragePct())
        .lastCalculatedAt(portfolio.getLastCalculatedAt())
        .createdAt(portfolio.getCreatedAt())
        .updatedAt(portfolio.getUpdatedAt())
        .build();
  }
}
