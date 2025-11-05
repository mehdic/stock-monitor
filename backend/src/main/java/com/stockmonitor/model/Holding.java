package com.stockmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "holding")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holding {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "portfolio_id", nullable = false)
  private UUID portfolioId;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String symbol;

  @NotNull
  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal quantity;

  @NotNull
  @Column(name = "cost_basis", nullable = false, precision = 19, scale = 4)
  private BigDecimal costBasis;

  @NotNull
  @Column(name = "cost_basis_per_share", nullable = false, precision = 19, scale = 4)
  private BigDecimal costBasisPerShare;

  @NotNull
  @Column(name = "acquisition_date", nullable = false)
  private LocalDate acquisitionDate;

  @NotBlank
  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "current_price", precision = 19, scale = 4, nullable = false)
  private BigDecimal currentPrice = BigDecimal.ZERO;

  @Column(name = "current_market_value", precision = 19, scale = 4, nullable = false)
  private BigDecimal currentMarketValue = BigDecimal.ZERO;

  @Column(name = "unrealized_pnl", precision = 19, scale = 4, nullable = false)
  private BigDecimal unrealizedPnl = BigDecimal.ZERO;

  @Column(name = "unrealized_pnl_pct", precision = 10, scale = 6, nullable = false)
  private BigDecimal unrealizedPnlPct = BigDecimal.ZERO;

  @Column(name = "weight_pct", precision = 5, scale = 2, nullable = false)
  private BigDecimal weightPct = BigDecimal.ZERO;

  @Column(name = "in_universe", nullable = false)
  private Boolean inUniverse = false;

  @Column(length = 50)
  private String sector;

  @Column(name = "market_cap_tier", length = 20)
  private String marketCapTier;

  @Column(name = "fx_rate_to_base", precision = 19, scale = 8, nullable = false)
  private BigDecimal fxRateToBase = BigDecimal.ONE;

  @Column(name = "price_updated_at")
  private LocalDateTime priceUpdatedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
