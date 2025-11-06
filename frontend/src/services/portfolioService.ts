import { apiClient } from './api';
import { Portfolio, Holding } from '../types';

/**
 * Portfolio service for managing portfolios and holdings.
 */
class PortfolioService {
  /**
   * Create a new portfolio.
   *
   * @param name Portfolio name
   * @param baseCurrency Base currency (default: USD)
   * @returns Created portfolio
   */
  async createPortfolio(name: string, baseCurrency: string = 'USD'): Promise<Portfolio> {
    const response = await apiClient.post<Portfolio>('/api/portfolios', {
      name,
      baseCurrency,
    });
    return response.data;
  }

  /**
   * Get portfolio by ID.
   *
   * @param portfolioId Portfolio ID
   * @returns Portfolio details
   */
  async getPortfolio(portfolioId: string): Promise<Portfolio> {
    const response = await apiClient.get<Portfolio>(`/api/portfolios/${portfolioId}`);
    return response.data;
  }

  /**
   * Get holdings for a portfolio.
   *
   * @param portfolioId Portfolio ID
   * @returns List of holdings
   */
  async getHoldings(portfolioId: string): Promise<Holding[]> {
    const response = await apiClient.get<Holding[]>(`/api/portfolios/${portfolioId}/holdings`);
    return response.data;
  }

  /**
   * Upload holdings via CSV file.
   *
   * @param portfolioId Portfolio ID
   * @param file CSV file with holdings
   * @returns Upload result
   */
  async uploadHoldings(portfolioId: string, file: File): Promise<{ message: string; holdingsCount: number }> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post(
      `/api/portfolios/${portfolioId}/holdings/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data;
  }

  /**
   * Update a single holding.
   *
   * @param portfolioId Portfolio ID
   * @param holdingId Holding ID
   * @param data Updated holding data
   * @returns Updated holding
   */
  async updateHolding(
    portfolioId: string,
    holdingId: string,
    data: Partial<Holding>
  ): Promise<Holding> {
    const response = await apiClient.put<Holding>(
      `/api/portfolios/${portfolioId}/holdings/${holdingId}`,
      data
    );
    return response.data;
  }

  /**
   * Delete a holding.
   *
   * @param portfolioId Portfolio ID
   * @param holdingId Holding ID
   */
  async deleteHolding(portfolioId: string, holdingId: string): Promise<void> {
    await apiClient.delete(`/api/portfolios/${portfolioId}/holdings/${holdingId}`);
  }

  /**
   * Get portfolio summary with calculated metrics.
   *
   * @param portfolioId Portfolio ID
   * @returns Portfolio summary
   */
  async getPortfolioSummary(portfolioId: string): Promise<{
    totalValue: number;
    marketValue: number;
    cash: number;
    unrealizedPnl: number;
    unrealizedPnlPct: number;
    holdingsCount: number;
  }> {
    const response = await apiClient.get(`/api/portfolios/${portfolioId}/summary`);
    return response.data;
  }

  /**
   * Get portfolio performance metrics.
   *
   * @param portfolioId Portfolio ID
   * @returns Performance metrics
   */
  async getPerformance(portfolioId: string): Promise<{
    totalReturn: number;
    totalReturnPct: number;
    topContributors: Holding[];
    topDetractors: Holding[];
  }> {
    const response = await apiClient.get(`/api/portfolios/${portfolioId}/performance`);
    return response.data;
  }
}

export const portfolioService = new PortfolioService();
