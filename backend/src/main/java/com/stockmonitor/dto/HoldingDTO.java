package com.stockmonitor.dto;

import com.stockmonitor.model.Holding;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class HoldingDTO {
  private UUID id;
  private UUID portfolioId;
  private String symbol;
  private BigDecimal quantity;
  private BigDecimal costBasis;
  private BigDecimal costBasisPerShare;
  private BigDecimal currentPrice;
  private BigDecimal currentMarketValue;
  private BigDecimal unrealizedPnl;
  private BigDecimal unrealizedPnlPct;
  private BigDecimal weightPct;
  private String currency;
  private LocalDate acquisitionDate;
  private Boolean inUniverse;
  private String sector;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static HoldingDTO from(Holding holding) {
    return HoldingDTO.builder()
        .id(holding.getId())
        .portfolioId(holding.getPortfolioId())
        .symbol(holding.getSymbol())
        .quantity(holding.getQuantity())
        .costBasis(holding.getCostBasis())
        .costBasisPerShare(holding.getCostBasisPerShare())
        .currentPrice(holding.getCurrentPrice())
        .currentMarketValue(holding.getCurrentMarketValue())
        .unrealizedPnl(holding.getUnrealizedPnl())
        .unrealizedPnlPct(holding.getUnrealizedPnlPct())
        .weightPct(holding.getWeightPct())
        .currency(holding.getCurrency())
        .acquisitionDate(holding.getAcquisitionDate())
        .inUniverse(holding.getInUniverse())
        .sector(holding.getSector())
        .createdAt(holding.getCreatedAt())
        .updatedAt(holding.getUpdatedAt())
        .build();
  }
}
