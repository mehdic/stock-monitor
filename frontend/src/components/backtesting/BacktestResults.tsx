import { BacktestDTO } from '../../services/backtestService';
import EquityCurveChart from './EquityCurveChart';
import PerformanceTable from './PerformanceTable';

interface BacktestResultsProps {
  backtest: BacktestDTO;
}

/**
 * Backtest results display with equity curve and performance metrics (T184).
 */
export default function BacktestResults({ backtest }: BacktestResultsProps) {
  return (
    <div className="space-y-6">
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-semibold text-blue-900">
          Backtest: {new Date(backtest.startDate).toLocaleDateString()} -{' '}
          {new Date(backtest.endDate).toLocaleDateString()}
        </h3>
        <p className="text-sm text-blue-800 mt-1">
          Status: {backtest.status}
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <EquityCurveChart equityCurve={backtest.equityCurve} />
        <PerformanceTable backtest={backtest} />
      </div>
    </div>
  );
}
