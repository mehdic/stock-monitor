package com.stockmonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingsUploadResponse {
  private Integer totalHoldings;
  private BigDecimal marketValue;
  private LocalDateTime uploadedAt;
  private List<ValidationError> validationErrors;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationError {
    private Integer row;
    private String column;
    private String errorCode;
    private String message;
  }
}
