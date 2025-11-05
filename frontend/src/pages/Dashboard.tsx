import { useQuery } from '@tanstack/react-query';
import { factorService } from '../services/factorService';
import ContributorsTable from '../components/portfolio/ContributorsTable';
import BenchmarkChart from '../components/portfolio/BenchmarkChart';

/**
 * Dashboard page with portfolio overview and performance metrics (T162, FR-008, FR-014).
 */
export default function Dashboard() {
  const portfolioId = 'default-portfolio'; // TODO: Get from context/route

  const { data: performance, isLoading } = useQuery({
    queryKey: ['performance', portfolioId],
    queryFn: () => factorService.getPerformance(portfolioId),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full"></div>
        <span className="ml-3 text-gray-600">Loading dashboard...</span>
      </div>
    );
  }

  if (!performance) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
        <p className="text-yellow-800">No performance data available</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Portfolio Dashboard</h1>
        <p className="text-gray-600 mt-2">
          Monitor performance, analyze holdings, and review recommendations
        </p>
      </div>

      {/* Performance Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Total P&L</h3>
          <p
            className={`text-2xl font-bold ${
              performance.totalPnL >= 0 ? 'text-green-600' : 'text-red-600'
            }`}
          >
            ${performance.totalPnL.toLocaleString(undefined, { minimumFractionDigits: 2 })}
          </p>
          <p className="text-xs text-gray-500 mt-1">
            {performance.totalPnLPct >= 0 ? '+' : ''}
            {performance.totalPnLPct.toFixed(2)}%
          </p>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Portfolio Value</h3>
          <p className="text-2xl font-bold text-gray-900">
            ${performance.endingValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
          </p>
          <p className="text-xs text-gray-500 mt-1">Current</p>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Excess Return</h3>
          <p
            className={`text-2xl font-bold ${
              (performance.excessReturn || 0) >= 0 ? 'text-green-600' : 'text-red-600'
            }`}
          >
            {(performance.excessReturn || 0) >= 0 ? '+' : ''}
            {(performance.excessReturn || 0).toFixed(2)}%
          </p>
          <p className="text-xs text-gray-500 mt-1">vs. S&P 500</p>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Trades</h3>
          <p className="text-2xl font-bold text-gray-900">{performance.tradeCount}</p>
          <p className="text-xs text-gray-500 mt-1">This period</p>
        </div>
      </div>

      {/* Benchmark Comparison */}
      <BenchmarkChart performance={performance} />

      {/* Contributors and Detractors */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ContributorsTable
          contributors={performance.topContributors}
          title="Top Contributors"
          type="contributor"
        />
        <ContributorsTable
          contributors={performance.topDetractors}
          title="Top Detractors"
          type="detractor"
        />
      </div>

      {/* Period Info */}
      <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600">
        Performance period: {new Date(performance.periodStart).toLocaleDateString()} -{' '}
        {new Date(performance.periodEnd).toLocaleDateString()}
      </div>
    </div>
  );
}
