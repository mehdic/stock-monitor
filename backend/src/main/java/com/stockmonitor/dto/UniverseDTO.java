package com.stockmonitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmonitor.model.Universe;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniverseDTO {
  private UUID id;
  private String name;
  private String description;
  private String type;
  private String benchmarkSymbol;
  private Integer constituentCount;

  @JsonProperty("marketCapMin")
  private BigDecimal minMarketCap;

  @JsonProperty("marketCapMax")
  private BigDecimal maxMarketCap;

  private String liquidityTierThreshold;
  private Boolean isActive;
  private Integer version;
  private LocalDate effectiveDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<String> tickerList;

  public static UniverseDTO from(Universe universe) {
    return UniverseDTO.builder()
        .id(universe.getId())
        .name(universe.getName())
        .description(universe.getDescription())
        .type(universe.getType())
        .benchmarkSymbol(universe.getBenchmarkSymbol())
        .constituentCount(universe.getConstituentCount())
        .minMarketCap(universe.getMinMarketCap())
        .maxMarketCap(universe.getMaxMarketCap())
        .liquidityTierThreshold(universe.getLiquidityTierThreshold())
        .isActive(universe.getIsActive())
        .version(universe.getVersion())
        .effectiveDate(universe.getEffectiveDate())
        .createdAt(universe.getCreatedAt())
        .updatedAt(universe.getUpdatedAt())
        .tickerList(List.of())
        .build();
  }
}
