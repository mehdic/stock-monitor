package com.stockmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for backtest-specific portfolio construction constraints.
 *
 * <p>Separate from ConstraintSetDTO (trading execution constraints) as backtesting
 * focuses on portfolio construction rules, not real-time execution constraints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestConstraintsDTO {
    /** Maximum position size as percentage of portfolio (e.g., 5.0 = 5%) */
    private Double maxPositionSizePct;

    /** Maximum sector exposure as percentage of portfolio (e.g., 20.0 = 20%) */
    private Double maxSectorExposurePct;

    /** Maximum turnover percentage per rebalance (e.g., 25.0 = 25%) */
    private Double maxTurnoverPct;

    /** Minimum market capitalization in billions (e.g., 1.0 = $1B) */
    private Double minMarketCapBn;

    /** Cash buffer percentage to maintain (e.g., 2.0 = 2%) */
    private Double cashBufferPct;

    /** Minimum liquidity tier (1-5, where 1 is most liquid) */
    private Integer minLiquidityTier;
}
