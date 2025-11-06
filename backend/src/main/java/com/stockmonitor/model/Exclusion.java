package com.stockmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "exclusion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exclusion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "run_id", nullable = false)
  private UUID runId;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String symbol;

  @NotBlank
  @Column(name = "exclusion_reason_code", nullable = false, length = 50)
  private String exclusionReasonCode;

  @NotBlank
  @Column(name = "exclusion_reason_text", nullable = false, length = 500)
  private String exclusionReasonText;

  @NotBlank
  @Column(nullable = false, length = 50)
  private String sector;

  @NotBlank
  @Column(name = "market_cap_tier", nullable = false, length = 20)
  private String marketCapTier;

  @NotNull
  @Column(name = "liquidity_tier", nullable = false)
  private Integer liquidityTier;

  @Column(name = "failed_constraint_name", length = 100)
  private String failedConstraintName;

  @Column(name = "failed_constraint_value", precision = 19, scale = 4)
  private BigDecimal failedConstraintValue;

  @Column(name = "failed_constraint_threshold", precision = 19, scale = 4)
  private BigDecimal failedConstraintThreshold;

  @NotNull
  @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal currentPrice;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
