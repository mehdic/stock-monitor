package com.stockmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingsUploadRequest {
  private String ticker;
  private Double quantity;
  private Double costBasis;
  private String currency;
}
