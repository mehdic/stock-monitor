import { apiClient } from './api';

/**
 * Factor score DTO (T169).
 */
export interface FactorScoreDTO {
  value: number;
  momentum: number;
  quality: number;
  revisions: number;
  composite: number;
  symbol: string;
  sector: string;
  calculatedAt: string;
  rawValue?: number;
  rawMomentum?: number;
  rawQuality?: number;
  rawRevisions?: number;
  valuePercentile?: number;
  momentumPercentile?: number;
  qualityPercentile?: number;
  revisionsPercentile?: number;
}

/**
 * Performance metrics DTO.
 */
export interface PerformanceMetricsDTO {
  totalPnL: number;
  totalPnLPct: number;
  benchmarkReturn?: number;
  excessReturn?: number;
  topContributors: PerformanceContributorDTO[];
  topDetractors: PerformanceContributorDTO[];
  periodStart: string;
  periodEnd: string;
  startingValue: number;
  endingValue: number;
  tradeCount: number;
  transactionCosts: number;
}

export interface PerformanceContributorDTO {
  symbol: string;
  pnl: number;
  pnlPct: number;
  weight: number;
  sector: string;
  shares: number;
  costBasis: number;
  currentPrice: number;
}

/**
 * Data source health DTO.
 */
export interface DataSourceHealthDTO {
  id: string;
  name: string;
  status: 'HEALTHY' | 'STALE' | 'UNAVAILABLE';
  lastUpdateTime: string;
  freshnessThresholdMinutes: number;
  minutesSinceUpdate: number;
  message: string;
  consecutiveFailures?: number;
  nextCheckTime?: string;
  sourceType?: string;
}

/**
 * Service for factor analysis and portfolio monitoring (T169).
 */
class FactorService {
  /**
   * Get factor scores for all holdings in portfolio.
   */
  async getPortfolioFactors(portfolioId: string): Promise<FactorScoreDTO[]> {
    const response = await apiClient.get<FactorScoreDTO[]>(
      `/portfolios/${portfolioId}/factors`
    );
    return response.data;
  }

  /**
   * Get factor scores for specific holding with detailed breakdown.
   */
  async getHoldingFactors(holdingId: string): Promise<FactorScoreDTO> {
    const response = await apiClient.get<FactorScoreDTO>(`/holdings/${holdingId}/factors`);
    return response.data;
  }

  /**
   * Get performance metrics for portfolio.
   */
  async getPerformance(
    portfolioId: string,
    startDate?: string,
    endDate?: string
  ): Promise<PerformanceMetricsDTO> {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);

    const response = await apiClient.get<PerformanceMetricsDTO>(
      `/portfolios/${portfolioId}/performance?${params.toString()}`
    );
    return response.data;
  }

  /**
   * Get all data sources with health status.
   */
  async getAllDataSources(): Promise<DataSourceHealthDTO[]> {
    const response = await apiClient.get<DataSourceHealthDTO[]>('/data-sources');
    return response.data;
  }

  /**
   * Get detailed health for specific data source.
   */
  async getDataSourceHealth(dataSourceId: string): Promise<DataSourceHealthDTO> {
    const response = await apiClient.get<DataSourceHealthDTO>(
      `/data-sources/${dataSourceId}/health`
    );
    return response.data;
  }
}

export const factorService = new FactorService();
