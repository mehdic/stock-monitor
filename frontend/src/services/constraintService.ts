import { apiClient } from './api';

export interface ConstraintSetDTO {
  id?: string;
  userId?: string;
  name?: string;
  maxPositionSizePct: number;
  maxSectorExposurePct: number;
  maxTurnoverPct: number;
  minLiquidityTier: number;
  minMarketCapBn: number;
  cashBufferPct: number;
  taxLossHarvestingEnabled?: boolean;
  minHoldingPeriodDays?: number;
  maxDrawdownThreshold?: number;
  maxNumHoldings?: number;
  minNumHoldings?: number;
  allowShortSelling?: boolean;
  sectorNeutral?: boolean;
  betaNeutral?: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConstraintPreviewDTO {
  expectedPickCount: number;
  expectedPickCountRange: string;
  expectedTurnoverPct: number;
  expectedTurnoverRange: string;
  affectedPositionsCount: number;
  droppedSymbols: string[];
  addedSymbols: string[];
  accuracyNote: string;
  constraintChangesSummary: string;
  warnings?: string[];
}

/**
 * Service for constraint management (T145).
 */
class ConstraintService {
  /**
   * Get default constraints.
   */
  async getDefaults(): Promise<ConstraintSetDTO> {
    const response = await apiClient.get<ConstraintSetDTO>('/constraints/defaults');
    return response.data;
  }

  /**
   * Get constraints for a portfolio.
   */
  async getConstraintsForPortfolio(portfolioId: string): Promise<ConstraintSetDTO> {
    const response = await apiClient.get<ConstraintSetDTO>(`/portfolios/${portfolioId}/constraints`);
    return response.data;
  }

  /**
   * Preview impact of constraint changes (FR-017).
   */
  async previewImpact(
    portfolioId: string,
    modifiedConstraints: ConstraintSetDTO
  ): Promise<ConstraintPreviewDTO> {
    const response = await apiClient.post<ConstraintPreviewDTO>(
      `/portfolios/${portfolioId}/constraints/preview`,
      modifiedConstraints
    );
    return response.data;
  }

  /**
   * Save constraint changes.
   */
  async saveConstraints(
    portfolioId: string,
    constraints: ConstraintSetDTO
  ): Promise<ConstraintSetDTO> {
    const response = await apiClient.put<ConstraintSetDTO>(
      `/portfolios/${portfolioId}/constraints`,
      constraints
    );
    return response.data;
  }

  /**
   * Reset constraints to defaults (FR-018).
   */
  async resetToDefaults(portfolioId: string): Promise<ConstraintSetDTO> {
    const response = await apiClient.post<ConstraintSetDTO>(
      `/portfolios/${portfolioId}/constraints/reset`
    );
    return response.data;
  }
}

export const constraintService = new ConstraintService();
