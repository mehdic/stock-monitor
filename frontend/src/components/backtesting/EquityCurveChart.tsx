import { EquityPoint } from '../../services/backtestService';

interface EquityCurveChartProps {
  equityCurve: EquityPoint[];
}

/**
 * Equity curve visualization for backtest results (T185).
 */
export default function EquityCurveChart({ equityCurve }: EquityCurveChartProps) {
  if (!equityCurve || equityCurve.length === 0) {
    return <div className="text-gray-500">No equity curve data available</div>;
  }

  // Simple line chart representation
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Equity Curve</h3>
      <p className="text-sm text-gray-600 mb-4">
        Portfolio value over time vs. benchmark
      </p>
      {/* TODO: Integrate with charting library (Chart.js, Recharts, etc.) */}
      <div className="h-64 bg-gray-50 rounded flex items-center justify-center">
        <p className="text-gray-500">Chart visualization placeholder</p>
      </div>
    </div>
  );
}
