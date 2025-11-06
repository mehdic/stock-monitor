package com.stockmonitor.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class TriggerRunRequest {
  @NotNull(message = "Portfolio ID is required")
  private UUID portfolioId;

  private UUID universeId; // Optional - will use portfolio's active universe if not provided

  private String runType; // Optional - defaults to OFF_CYCLE
}
