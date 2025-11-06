import { PerformanceMetricsDTO } from '../../services/factorService';

interface BenchmarkChartProps {
  performance: PerformanceMetricsDTO;
}

/**
 * Chart comparing portfolio performance to benchmark (T164, FR-014).
 */
export default function BenchmarkChart({ performance }: BenchmarkChartProps) {
  const portfolioReturn = performance.totalPnLPct;
  const benchmarkReturn = performance.benchmarkReturn || 0;
  const excessReturn = performance.excessReturn || portfolioReturn - benchmarkReturn;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-6">Benchmark Comparison</h3>

      {/* Returns Bar Chart */}
      <div className="space-y-4">
        <div>
          <div className="flex justify-between mb-2">
            <span className="text-sm font-medium text-gray-700">Portfolio Return</span>
            <span className={`text-sm font-bold ${portfolioReturn >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {portfolioReturn >= 0 ? '+' : ''}{portfolioReturn.toFixed(2)}%
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3">
            <div
              className={`h-3 rounded-full ${portfolioReturn >= 0 ? 'bg-green-500' : 'bg-red-500'}`}
              style={{ width: `${Math.min(Math.abs(portfolioReturn) * 5, 100)}%` }}
            ></div>
          </div>
        </div>

        <div>
          <div className="flex justify-between mb-2">
            <span className="text-sm font-medium text-gray-700">Benchmark (S&P 500)</span>
            <span className={`text-sm font-bold ${benchmarkReturn >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {benchmarkReturn >= 0 ? '+' : ''}{benchmarkReturn.toFixed(2)}%
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3">
            <div
              className={`h-3 rounded-full ${benchmarkReturn >= 0 ? 'bg-blue-500' : 'bg-orange-500'}`}
              style={{ width: `${Math.min(Math.abs(benchmarkReturn) * 5, 100)}%` }}
            ></div>
          </div>
        </div>
      </div>

      {/* Excess Return */}
      <div className="mt-6 pt-6 border-t border-gray-200">
        <div className="flex justify-between items-center">
          <span className="text-sm font-medium text-gray-700">Excess Return (Alpha)</span>
          <span className={`text-lg font-bold ${excessReturn >= 0 ? 'text-green-600' : 'text-red-600'}`}>
            {excessReturn >= 0 ? '+' : ''}{excessReturn.toFixed(2)}%
          </span>
        </div>
        <p className="text-xs text-gray-500 mt-2">
          {excessReturn >= 0
            ? 'Portfolio outperformed benchmark'
            : 'Portfolio underperformed benchmark'}
        </p>
      </div>

      {/* Period Info */}
      <div className="mt-4 text-xs text-gray-500">
        Period: {new Date(performance.periodStart).toLocaleDateString()} -{' '}
        {new Date(performance.periodEnd).toLocaleDateString()}
      </div>
    </div>
  );
}
