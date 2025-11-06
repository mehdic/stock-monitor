import { useEffect, useState } from 'react';
import { portfolioService } from '../../services/portfolioService';

interface PortfolioSummaryProps {
  portfolioId: string;
  refreshTrigger?: number;
}

interface SummaryData {
  totalValue: number;
  marketValue: number;
  cash: number;
  unrealizedPnl: number;
  unrealizedPnlPct: number;
  holdingsCount: number;
}

export default function PortfolioSummary({ portfolioId, refreshTrigger = 0 }: PortfolioSummaryProps) {
  const [summary, setSummary] = useState<SummaryData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSummary();
  }, [portfolioId, refreshTrigger]);

  const fetchSummary = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await portfolioService.getPortfolioSummary(portfolioId);
      setSummary(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load portfolio summary');
      console.error('Failed to fetch portfolio summary:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatPercent = (value: number): string => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-4 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-16 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          <p className="font-medium">Error Loading Summary</p>
          <p className="mt-1">{error}</p>
          <button
            onClick={fetchSummary}
            className="mt-2 text-sm text-red-800 hover:text-red-900 underline"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  const pnlPositive = summary.unrealizedPnl >= 0;

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Portfolio Summary</h2>
      </div>

      <div className="p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* Total Value */}
          <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-lg p-4">
            <p className="text-sm font-medium text-blue-800">Total Value</p>
            <p className="text-2xl font-bold text-blue-900 mt-1">{formatCurrency(summary.totalValue)}</p>
            <p className="text-xs text-blue-700 mt-1">{summary.holdingsCount} holdings</p>
          </div>

          {/* Market Value */}
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm font-medium text-gray-600">Market Value</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(summary.marketValue)}</p>
            <p className="text-xs text-gray-500 mt-1">Invested capital</p>
          </div>

          {/* Cash */}
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm font-medium text-gray-600">Cash</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(summary.cash)}</p>
            <p className="text-xs text-gray-500 mt-1">Available balance</p>
          </div>

          {/* Unrealized P&L */}
          <div
            className={`rounded-lg p-4 ${
              pnlPositive ? 'bg-gradient-to-br from-green-50 to-green-100' : 'bg-gradient-to-br from-red-50 to-red-100'
            }`}
          >
            <p className={`text-sm font-medium ${pnlPositive ? 'text-green-800' : 'text-red-800'}`}>
              Unrealized P&L
            </p>
            <p className={`text-2xl font-bold mt-1 ${pnlPositive ? 'text-green-900' : 'text-red-900'}`}>
              {formatCurrency(summary.unrealizedPnl)}
            </p>
            <p className={`text-xs mt-1 ${pnlPositive ? 'text-green-700' : 'text-red-700'}`}>
              {formatPercent(summary.unrealizedPnlPct)}
            </p>
          </div>

          {/* Performance Badge */}
          <div className="bg-gray-50 rounded-lg p-4 flex items-center justify-center">
            <div className="text-center">
              <div
                className={`inline-flex items-center justify-center w-16 h-16 rounded-full ${
                  pnlPositive ? 'bg-green-100' : 'bg-red-100'
                }`}
              >
                {pnlPositive ? (
                  <svg className="w-8 h-8 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M12 7a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0V8.414l-4.293 4.293a1 1 0 01-1.414 0L8 10.414l-4.293 4.293a1 1 0 01-1.414-1.414l5-5a1 1 0 011.414 0L11 10.586 14.586 7H12z"
                      clipRule="evenodd"
                    />
                  </svg>
                ) : (
                  <svg className="w-8 h-8 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M12 13a1 1 0 100 2h5a1 1 0 001-1v-5a1 1 0 10-2 0v2.586l-4.293-4.293a1 1 0 00-1.414 0L8 9.586 3.707 5.293a1 1 0 00-1.414 1.414l5 5a1 1 0 001.414 0L11 9.414 14.586 13H12z"
                      clipRule="evenodd"
                    />
                  </svg>
                )}
              </div>
              <p className={`text-sm font-medium mt-2 ${pnlPositive ? 'text-green-700' : 'text-red-700'}`}>
                {pnlPositive ? 'Profitable' : 'Underwater'}
              </p>
            </div>
          </div>

          {/* Holdings Count */}
          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm font-medium text-gray-600">Holdings</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{summary.holdingsCount}</p>
            <p className="text-xs text-gray-500 mt-1">Active positions</p>
          </div>
        </div>
      </div>
    </div>
  );
}
