import { apiClient } from './api';
import { Recommendation } from '../components/recommendations/RecommendationCard';

export interface RecommendationRun {
  id: string;
  portfolioId: string;
  universeId: string;
  constraintSetId: string;
  runType: 'SCHEDULED' | 'OFF_CYCLE';
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  completedAt?: string;
  errorMessage?: string;
}

export interface RecommendationRunDetails extends RecommendationRun {
  recommendations: Recommendation[];
  summary: {
    totalRecommendations: number;
    buyCount: number;
    sellCount: number;
    holdCount: number;
    avgConfidence: number;
    constraintViolations: number;
  };
}

/**
 * Recommendation service for managing portfolio optimization runs and recommendations.
 */
class RecommendationService {
  /**
   * Trigger a new recommendation run.
   *
   * @param portfolioId Portfolio ID
   * @param universeId Universe ID
   * @param runType Run type (SCHEDULED or OFF_CYCLE), defaults to OFF_CYCLE
   * @returns Created run details
   */
  async triggerRun(
    portfolioId: string,
    universeId: string,
    runType: 'SCHEDULED' | 'OFF_CYCLE' = 'OFF_CYCLE'
  ): Promise<RecommendationRun> {
    const response = await apiClient.post<RecommendationRun>('/api/runs', null, {
      params: {
        portfolioId,
        universeId,
        runType,
      },
    });
    return response.data;
  }

  /**
   * Get run details by ID.
   *
   * @param runId Run ID
   * @returns Run details with recommendations
   */
  async getRun(runId: string): Promise<RecommendationRunDetails> {
    const response = await apiClient.get<RecommendationRunDetails>(`/api/runs/${runId}`);
    return response.data;
  }

  /**
   * Get all runs for a portfolio.
   *
   * @param portfolioId Portfolio ID
   * @param runType Optional filter by run type
   * @returns List of runs
   */
  async getRunsForPortfolio(portfolioId: string, runType?: 'SCHEDULED' | 'OFF_CYCLE'): Promise<RecommendationRun[]> {
    const response = await apiClient.get<RecommendationRun[]>('/api/runs', {
      params: {
        portfolioId,
        runType,
      },
    });
    return response.data;
  }

  /**
   * Get latest run for a portfolio.
   *
   * @param portfolioId Portfolio ID
   * @param runType Optional filter by run type
   * @returns Latest run or null if no runs exist
   */
  async getLatestRun(portfolioId: string, runType?: 'SCHEDULED' | 'OFF_CYCLE'): Promise<RecommendationRun | null> {
    const runs = await this.getRunsForPortfolio(portfolioId, runType);
    if (runs.length === 0) return null;

    // Sort by startedAt descending
    runs.sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime());
    return runs[0];
  }

  /**
   * Get recommendations for a run.
   *
   * @param runId Run ID
   * @returns List of recommendations
   */
  async getRecommendations(runId: string): Promise<Recommendation[]> {
    const response = await apiClient.get<Recommendation[]>(`/api/runs/${runId}/recommendations`);
    return response.data;
  }

  /**
   * Poll run status until completion.
   *
   * @param runId Run ID
   * @param pollInterval Polling interval in milliseconds (default: 2000)
   * @param maxAttempts Maximum polling attempts (default: 60, i.e., 2 minutes)
   * @returns Completed run details
   * @throws Error if run fails or times out
   */
  async pollRunStatus(
    runId: string,
    pollInterval: number = 2000,
    maxAttempts: number = 60
  ): Promise<RecommendationRun> {
    let attempts = 0;

    while (attempts < maxAttempts) {
      const run = await this.getRun(runId);

      if (run.status === 'COMPLETED') {
        return run;
      }

      if (run.status === 'FAILED') {
        throw new Error(run.errorMessage || 'Recommendation run failed');
      }

      // Still running, wait and retry
      await new Promise((resolve) => setTimeout(resolve, pollInterval));
      attempts++;
    }

    throw new Error('Recommendation run timed out');
  }

  /**
   * Get run summary statistics.
   *
   * @param runId Run ID
   * @returns Summary statistics
   */
  async getRunSummary(runId: string): Promise<RecommendationRunDetails['summary']> {
    const run = await this.getRun(runId);
    return run.summary;
  }

  /**
   * Cancel a running recommendation run.
   *
   * @param runId Run ID
   */
  async cancelRun(runId: string): Promise<void> {
    await apiClient.delete(`/api/runs/${runId}`);
  }

  /**
   * Get data freshness information for a run.
   *
   * @param runId Run ID
   * @returns Data freshness info
   */
  async getDataFreshness(runId: string): Promise<{
    marketDataAsOf: string;
    factorScoresAsOf: string;
    universeAsOf: string;
    staleness: number; // hours since market data update
  }> {
    const response = await apiClient.get(`/api/runs/${runId}/data-freshness`);
    return response.data;
  }

  /**
   * Get report data (JSON) for a run.
   *
   * @param runId Run ID
   * @returns Report data as JSON
   */
  async getReportJSON(runId: string): Promise<any> {
    const response = await apiClient.get(`/runs/${runId}/report`);
    return response.data;
  }

  /**
   * Download report as PDF.
   *
   * @param runId Run ID
   * @returns Blob containing PDF data
   */
  async downloadReportPDF(runId: string): Promise<Blob> {
    const response = await apiClient.get(`/runs/${runId}/report/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  }

  /**
   * Download report as PDF and trigger browser download.
   *
   * @param runId Run ID
   * @param filename Optional custom filename
   */
  async downloadReport(runId: string, filename?: string): Promise<void> {
    const blob = await this.downloadReportPDF(runId);

    // Create download link
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || `recommendation-report-${runId.substring(0, 8)}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }
}

export const recommendationService = new RecommendationService();
