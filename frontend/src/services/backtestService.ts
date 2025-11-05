import { apiClient } from './api';
import { ConstraintSetDTO } from './constraintService';

export interface BacktestDTO {
  backtestId: string;
  portfolioId: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startDate: string;
  endDate: string;
  cagr: number;
  sharpeRatio: number;
  maxDrawdown: number;
  equityCurve: EquityPoint[];
  averageTurnover: number;
  tradeCount: number;
  benchmarkCAGR: number;
  beatEqualWeight: boolean;
  totalTransactionCosts: number;
  estimatedCompletion?: string;
  errorMessage?: string;
}

export interface EquityPoint {
  date: string;
  portfolioValue: number;
  benchmarkValue: number;
}

export interface SensitivityPreviewDTO {
  constraintName: string;
  originalValue: number;
  newValue: number;
  expectedHoldingsDelta: number;
  expectedTurnoverDelta: number;
  expectedSectorConcentrationDelta: number;
  expectedReturnDelta: number;
  expectedRiskDelta: number;
  sensitivityScore: number;
  impactSummary: string;
  recommendation: string;
}

/**
 * Service for backtesting and sensitivity analysis (T187).
 */
class BacktestService {
  /**
   * Run backtest with given parameters.
   */
  async runBacktest(
    portfolioId: string,
    startDate: string,
    endDate: string,
    constraints: ConstraintSetDTO
  ): Promise<BacktestDTO> {
    const response = await apiClient.post<BacktestDTO>('/backtests', {
      portfolioId,
      startDate,
      endDate,
      constraints,
    });
    return response.data;
  }

  /**
   * Get backtest results by ID.
   */
  async getBacktest(backtestId: string): Promise<BacktestDTO> {
    const response = await apiClient.get<BacktestDTO>(`/backtests/${backtestId}`);
    return response.data;
  }

  /**
   * Analyze sensitivity of constraint change.
   */
  async analyzeSensitivity(
    portfolioId: string,
    constraintName: string,
    newValue: number,
    currentConstraints: ConstraintSetDTO
  ): Promise<SensitivityPreviewDTO> {
    const response = await apiClient.post<SensitivityPreviewDTO>(
      '/constraints/sensitivity',
      {
        portfolioId,
        constraintName,
        newValue,
        currentConstraints,
      }
    );
    return response.data;
  }
}

export const backtestService = new BacktestService();
