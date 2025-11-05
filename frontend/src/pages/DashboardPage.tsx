import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../services/api';
import { Portfolio, RecommendationRun } from '../types';

export default function DashboardPage() {
  // Fetch portfolio summary
  const { data: portfolio } = useQuery({
    queryKey: ['portfolio'],
    queryFn: async () => {
      const portfolioId = localStorage.getItem('portfolioId') || 'temp-id';
      const response = await apiClient.get<Portfolio>(`/portfolios/${portfolioId}`);
      return response.data;
    },
  });

  // Fetch latest recommendation run
  const { data: latestRun } = useQuery({
    queryKey: ['latest-run'],
    queryFn: async () => {
      const userId = localStorage.getItem('userId') || 'temp-user-id';
      const response = await apiClient.get<RecommendationRun[]>(`/users/${userId}/runs`);
      return response.data[0]; // Most recent
    },
  });

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">Portfolio overview and quick actions</p>
      </div>

      {/* Quick Stats */}
      {portfolio && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <p className="text-sm font-medium text-gray-600">Total Value</p>
            <p className="mt-2 text-3xl font-semibold text-gray-900">
              {formatCurrency(portfolio.totalValue)}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <p className="text-sm font-medium text-gray-600">Market Value</p>
            <p className="mt-2 text-3xl font-semibold text-gray-900">
              {formatCurrency(portfolio.marketValue)}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <p className="text-sm font-medium text-gray-600">Cash</p>
            <p className="mt-2 text-3xl font-semibold text-gray-900">
              {formatCurrency(portfolio.cash)}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <p className="text-sm font-medium text-gray-600">Unrealized P&L</p>
            <p className={`mt-2 text-3xl font-semibold ${
              portfolio.unrealizedPnl >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {formatCurrency(portfolio.unrealizedPnl)}
            </p>
          </div>
        </div>
      )}

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow p-6 mb-8">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            to="/portfolio"
            className="p-6 border-2 border-gray-200 rounded-lg hover:border-blue-500 transition"
          >
            <h3 className="font-semibold text-gray-900 mb-2">Upload Holdings</h3>
            <p className="text-sm text-gray-600">
              Upload your portfolio holdings via CSV file
            </p>
          </Link>
          <Link
            to="/recommendations"
            className="p-6 border-2 border-gray-200 rounded-lg hover:border-blue-500 transition"
          >
            <h3 className="font-semibold text-gray-900 mb-2">Generate Recommendations</h3>
            <p className="text-sm text-gray-600">
              Run factor-based analysis for portfolio recommendations
            </p>
          </Link>
          <Link
            to="/settings"
            className="p-6 border-2 border-gray-200 rounded-lg hover:border-blue-500 transition"
          >
            <h3 className="font-semibold text-gray-900 mb-2">Configure Constraints</h3>
            <p className="text-sm text-gray-600">
              Set position limits, sector exposure, and risk constraints
            </p>
          </Link>
        </div>
      </div>

      {/* Latest Recommendation Run */}
      {latestRun && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Latest Recommendation Run</h2>
            <Link
              to="/recommendations"
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              View All →
            </Link>
          </div>
          <div className="border border-gray-200 rounded-lg p-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-xs text-gray-600">Date</p>
                <p className="font-medium text-gray-900">
                  {new Date(latestRun.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div>
                <p className="text-xs text-gray-600">Status</p>
                <p className="font-medium text-gray-900">{latestRun.status}</p>
              </div>
              <div>
                <p className="text-xs text-gray-600">Recommendations</p>
                <p className="font-medium text-gray-900">
                  {latestRun.recommendationCount}
                </p>
              </div>
              <div>
                <p className="text-xs text-gray-600">Expected Alpha</p>
                <p className="font-medium text-green-600">
                  {latestRun.expectedAlphaBps?.toFixed(0) || 0} bps
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Getting Started */}
      {!portfolio && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h2 className="text-xl font-semibold text-blue-900 mb-4">Getting Started</h2>
          <ol className="space-y-3 text-blue-800">
            <li className="flex items-start">
              <span className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-semibold mr-3">
                1
              </span>
              <div>
                <strong>Upload your portfolio holdings</strong> - Go to Portfolio page and upload a CSV file
              </div>
            </li>
            <li className="flex items-start">
              <span className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-semibold mr-3">
                2
              </span>
              <div>
                <strong>Select a universe</strong> - Choose from SP500, SP400, or SP600
              </div>
            </li>
            <li className="flex items-start">
              <span className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-semibold mr-3">
                3
              </span>
              <div>
                <strong>Generate recommendations</strong> - Run factor analysis to get ranked recommendations
              </div>
            </li>
          </ol>
        </div>
      )}
    </div>
  );
}
