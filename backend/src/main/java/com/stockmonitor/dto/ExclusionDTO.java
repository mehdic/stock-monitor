package com.stockmonitor.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for stock exclusions (T193, FR-031, FR-032).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExclusionDTO {

  private String symbol;
  private String companyName;
  private String exclusionReasonCode;
  private String explanation;
  private LocalDate runDate;

  /** Exclusion reason codes. */
  public static class ReasonCode {
    public static final String LIQUIDITY_FLOOR = "LIQUIDITY_FLOOR";
    public static final String SECTOR_CAP = "SECTOR_CAP";
    public static final String EARNINGS_PROXIMITY = "EARNINGS_PROXIMITY";
    public static final String SPREAD_THRESHOLD = "SPREAD_THRESHOLD";
    public static final String MARKET_CAP_FLOOR = "MARKET_CAP_FLOOR";
    public static final String POSITION_SIZE_CAP = "POSITION_SIZE_CAP";
  }
}
