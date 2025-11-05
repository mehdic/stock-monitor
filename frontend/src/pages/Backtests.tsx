import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { backtestService, BacktestDTO } from '../services/backtestService';
import { constraintService } from '../services/constraintService';
import BacktestResults from '../components/backtesting/BacktestResults';

/**
 * Backtests page for running historical simulations (T183).
 */
export default function Backtests() {
  const portfolioId = 'default-portfolio';
  const [startDate, setStartDate] = useState('2022-01-01');
  const [endDate, setEndDate] = useState('2024-01-01');
  const [backtestResult, setBacktestResult] = useState<BacktestDTO | null>(null);

  const { data: constraints } = useQuery({
    queryKey: ['constraints', portfolioId],
    queryFn: () => constraintService.getConstraintsForPortfolio(portfolioId),
  });

  const runBacktestMutation = useMutation({
    mutationFn: () =>
      backtestService.runBacktest(portfolioId, startDate, endDate, constraints!),
    onSuccess: (data) => setBacktestResult(data),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Backtesting</h1>
        <p className="text-gray-600 mt-2">
          Test historical performance of current strategy
        </p>
      </div>

      {/* Backtest Parameters */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Backtest Parameters</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Start Date
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              End Date
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
            />
          </div>
        </div>
        <button
          onClick={() => runBacktestMutation.mutate()}
          disabled={runBacktestMutation.isPending || !constraints}
          className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300"
        >
          {runBacktestMutation.isPending ? 'Running Backtest...' : 'Run Backtest'}
        </button>
      </div>

      {/* Results */}
      {backtestResult && <BacktestResults backtest={backtestResult} />}
    </div>
  );
}
