import { BacktestDTO } from '../../services/backtestService';

interface PerformanceTableProps {
  backtest: BacktestDTO;
}

/**
 * Performance metrics table for backtest (T186).
 */
export default function PerformanceTable({ backtest }: PerformanceTableProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Metrics</h3>
      <table className="min-w-full">
        <tbody className="divide-y divide-gray-200">
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">CAGR</td>
            <td className="py-3 text-sm text-right text-gray-900">{backtest.cagr.toFixed(2)}%</td>
          </tr>
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">Sharpe Ratio</td>
            <td className="py-3 text-sm text-right text-gray-900">{backtest.sharpeRatio.toFixed(2)}</td>
          </tr>
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">Max Drawdown</td>
            <td className="py-3 text-sm text-right text-red-600">{backtest.maxDrawdown.toFixed(2)}%</td>
          </tr>
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">Avg Turnover</td>
            <td className="py-3 text-sm text-right text-gray-900">{backtest.averageTurnover.toFixed(1)}%</td>
          </tr>
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">Trades</td>
            <td className="py-3 text-sm text-right text-gray-900">{backtest.tradeCount}</td>
          </tr>
          <tr>
            <td className="py-3 text-sm font-medium text-gray-700">Transaction Costs</td>
            <td className="py-3 text-sm text-right text-gray-900">
              ${backtest.totalTransactionCosts.toLocaleString()}
            </td>
          </tr>
          <tr className="bg-gray-50">
            <td className="py-3 text-sm font-medium text-gray-700">Benchmark CAGR</td>
            <td className="py-3 text-sm text-right text-gray-900">{backtest.benchmarkCAGR.toFixed(2)}%</td>
          </tr>
          <tr className="bg-green-50">
            <td className="py-3 text-sm font-bold text-gray-900">Beat Equal Weight?</td>
            <td className="py-3 text-sm text-right font-bold text-green-600">
              {backtest.beatEqualWeight ? 'Yes' : 'No'}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
