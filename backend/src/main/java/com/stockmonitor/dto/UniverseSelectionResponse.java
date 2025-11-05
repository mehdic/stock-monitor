package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniverseSelectionResponse {
  private UUID selectedUniverseId;
  private BigDecimal coveragePercentage;
  private Integer holdingsInUniverse;
  private Integer totalHoldings;
}
