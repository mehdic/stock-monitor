import { useQuery } from '@tanstack/react-query';
import { factorService } from '../services/factorService';
import FactorHeatmap from '../components/analysis/FactorHeatmap';
import FreshnessIndicators from '../components/analysis/FreshnessIndicators';

/**
 * Analysis page with factor heatmap and data freshness indicators (T165, FR-034 to FR-038).
 */
export default function Analysis() {
  const portfolioId = 'default-portfolio'; // TODO: Get from context/route

  const { data: factorScores, isLoading, error } = useQuery({
    queryKey: ['portfolioFactors', portfolioId],
    queryFn: () => factorService.getPortfolioFactors(portfolioId),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full"></div>
        <span className="ml-3 text-gray-600">Loading factor analysis...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
        <p className="text-red-800">Failed to load factor analysis: {String(error)}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Factor Analysis</h1>
        <p className="text-gray-600 mt-2">
          Analyze factor exposures and sector-normalized scores for portfolio holdings
        </p>
      </div>

      {/* Data Freshness Indicators */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1">
          <FreshnessIndicators />
        </div>

        <div className="lg:col-span-3">
          {/* Factor Summary Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-lg shadow p-4">
              <h4 className="text-sm font-medium text-gray-500 mb-1">Value</h4>
              <p className="text-xs text-gray-600">Valuation ratios</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <h4 className="text-sm font-medium text-gray-500 mb-1">Momentum</h4>
              <p className="text-xs text-gray-600">Price trends</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <h4 className="text-sm font-medium text-gray-500 mb-1">Quality</h4>
              <p className="text-xs text-gray-600">Profitability</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4">
              <h4 className="text-sm font-medium text-gray-500 mb-1">Revisions</h4>
              <p className="text-xs text-gray-600">Analyst changes</p>
            </div>
          </div>
        </div>
      </div>

      {/* Factor Heatmap */}
      {factorScores && <FactorHeatmap factorScores={factorScores} />}

      {/* Info Panel */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-blue-900 mb-2">About Factor Scores</h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>
            • <strong>Z-scores</strong>: Sector-normalized scores showing how far from sector
            average (mean=0, std=1)
          </li>
          <li>
            • <strong>Positive scores</strong>: Above sector average (favorable)
          </li>
          <li>
            • <strong>Negative scores</strong>: Below sector average
          </li>
          <li>
            • <strong>Composite score</strong>: Equal-weighted average of all four factors
          </li>
        </ul>
      </div>
    </div>
  );
}
