import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/api';
import { Recommendation, RecommendationRun, Universe } from '../types';
import { disclaimerService } from '../services/disclaimerService';
import DisclaimerModal from '../components/common/DisclaimerModal';
import RunStatusBadge from '../components/common/RunStatusBadge';
import ProgressBar from '../components/common/ProgressBar';
import ChangeIndicatorBadge from '../components/common/ChangeIndicatorBadge';
import useRunStatusWebSocket from '../hooks/useRunStatusWebSocket';

export default function RecommendationsPage() {
  const [selectedUniverse, setSelectedUniverse] = useState<string>('');
  const [selectedRunId, setSelectedRunId] = useState<string>('');
  const [showDisclaimer, setShowDisclaimer] = useState(false);
  const [runningRunId, setRunningRunId] = useState<string | null>(null);
  const queryClient = useQueryClient();

  // WebSocket connection for run status updates
  const { statusUpdate } = useRunStatusWebSocket({
    runId: runningRunId,
    onStatusUpdate: (update) => {
      // Invalidate queries when run completes
      if (update.status === 'COMPLETED' || update.status === 'FAILED') {
        queryClient.invalidateQueries({ queryKey: ['recommendation-runs'] });
        queryClient.invalidateQueries({ queryKey: ['recommendations', update.runId] });
        setRunningRunId(null);
      }
    },
  });

  // Check disclaimer acceptance on mount
  useEffect(() => {
    if (!disclaimerService.hasAccepted()) {
      setShowDisclaimer(true);
    }
  }, []);

  // Fetch universes
  const { data: universes } = useQuery({
    queryKey: ['universes'],
    queryFn: async () => {
      const response = await apiClient.get<Universe[]>('/universes');
      return response.data;
    },
  });

  // Fetch recommendation runs
  const { data: runs } = useQuery({
    queryKey: ['recommendation-runs'],
    queryFn: async () => {
      const userId = localStorage.getItem('userId') || 'temp-user-id';
      const response = await apiClient.get<RecommendationRun[]>(`/users/${userId}/runs`);
      return response.data;
    },
  });

  // Check for running runs on mount or when runs change
  useEffect(() => {
    const runningRun = runs?.find((run) => run.status === 'RUNNING');
    if (runningRun && runningRun.id !== runningRunId) {
      setRunningRunId(runningRun.id);
    }
  }, [runs, runningRunId]);

  // Fetch recommendations for selected run
  const { data: recommendations, isLoading: recommendationsLoading } = useQuery({
    queryKey: ['recommendations', selectedRunId],
    queryFn: async () => {
      if (!selectedRunId) return [];
      const response = await apiClient.get<Recommendation[]>(`/runs/${selectedRunId}/recommendations`);
      return response.data;
    },
    enabled: !!selectedRunId,
  });

  // Trigger new run mutation
  const triggerRunMutation = useMutation({
    mutationFn: async () => {
      const portfolioId = localStorage.getItem('portfolioId') || 'temp-id';
      const response = await apiClient.post<RecommendationRun>(
        '/runs',
        null,
        {
          params: {
            portfolioId,
            universeId: selectedUniverse,
          },
        }
      );
      return response.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['recommendation-runs'] });
      setSelectedRunId(data.id);
      setRunningRunId(data.id); // Connect WebSocket to new run
    },
  });

  const getConfidenceColor = (score: number) => {
    if (score >= 80) return 'text-green-600';
    if (score >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  const handleDisclaimerAccept = () => {
    setShowDisclaimer(false);
  };

  const handleDisclaimerDecline = () => {
    // Redirect user away from recommendations page if they decline
    window.location.href = '/dashboard';
  };

  return (
    <>
      {/* Disclaimer Modal */}
      <DisclaimerModal
        isOpen={showDisclaimer}
        onAccept={handleDisclaimerAccept}
        onDecline={handleDisclaimerDecline}
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Recommendations</h1>
        <p className="mt-2 text-gray-600">Factor-based portfolio recommendations</p>
      </div>

      {/* Trigger New Run Section */}
      <div className="bg-white rounded-lg shadow p-6 mb-8">
        <h2 className="text-xl font-semibold mb-4">Generate New Recommendations</h2>
        <div className="flex items-end space-x-4">
          <div className="flex-1">
            <label htmlFor="universe" className="block text-sm font-medium text-gray-700 mb-2">
              Select Universe
            </label>
            <select
              id="universe"
              value={selectedUniverse}
              onChange={(e) => setSelectedUniverse(e.target.value)}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Choose a universe...</option>
              {universes?.map((universe) => (
                <option key={universe.id} value={universe.id}>
                  {universe.name} ({universe.constituentCount} stocks)
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={() => triggerRunMutation.mutate()}
            disabled={!selectedUniverse || triggerRunMutation.isPending}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {triggerRunMutation.isPending ? 'Generating...' : 'Generate Recommendations'}
          </button>
        </div>
      </div>

      {/* Real-time Progress Display */}
      {statusUpdate && statusUpdate.status === 'RUNNING' && (
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Generation In Progress</h2>
            <RunStatusBadge status={statusUpdate.status} />
          </div>
          <ProgressBar
            progress={statusUpdate.progress}
            status="running"
            message={statusUpdate.stage}
            showPercentage={true}
            size="md"
          />
        </div>
      )}

      {/* Run History */}
      <div className="bg-white rounded-lg shadow p-6 mb-8">
        <h2 className="text-xl font-semibold mb-4">Recommendation History</h2>
        <div className="space-y-2">
          {runs?.slice(0, 5).map((run) => (
            <div
              key={run.id}
              onClick={() => setSelectedRunId(run.id)}
              className={`p-4 rounded-lg cursor-pointer border-2 transition ${
                selectedRunId === run.id
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <p className="font-medium text-gray-900">
                      Run from {new Date(run.createdAt).toLocaleDateString()}
                    </p>
                    <RunStatusBadge status={run.status} size="sm" />
                  </div>
                  <p className="text-sm text-gray-600">
                    {run.recommendationCount} recommendations
                  </p>
                </div>
                <div className="text-right">
                  {run.expectedAlphaBps && (
                    <p className="text-sm text-gray-600">
                      Expected Alpha: {run.expectedAlphaBps.toFixed(0)} bps
                    </p>
                  )}
                  {run.estimatedCostBps && (
                    <p className="text-sm text-gray-600">
                      Est. Cost: {run.estimatedCostBps.toFixed(0)} bps
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
          {(!runs || runs.length === 0) && (
            <p className="text-center text-gray-500 py-4">
              No recommendation runs yet. Generate your first recommendations above.
            </p>
          )}
        </div>
      </div>

      {/* Recommendations Table */}
      {selectedRunId && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-xl font-semibold">
              Ranked Recommendations ({recommendations?.length || 0})
            </h2>
          </div>

          {recommendationsLoading ? (
            <div className="p-8 text-center text-gray-600">Loading recommendations...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Rank
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Symbol
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Action
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Target Wt%
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Change
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Confidence
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Expected Alpha
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Edge/Cost
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Top Drivers
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {recommendations?.map((rec) => (
                    <tr key={rec.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        #{rec.rank}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{rec.symbol}</div>
                        <div className="text-xs text-gray-500">{rec.sector}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <ChangeIndicatorBadge
                          changeIndicator={rec.changeIndicator}
                          weightChange={rec.weightChangePct}
                          size="sm"
                          showIcon={true}
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                        {rec.targetWeightPct.toFixed(2)}%
                      </td>
                      <td className={`px-6 py-4 whitespace-nowrap text-sm text-right ${
                        rec.weightChangePct >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {rec.weightChangePct >= 0 ? '+' : ''}{rec.weightChangePct.toFixed(2)}%
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <span className={`text-sm font-semibold ${getConfidenceColor(rec.confidenceScore)}`}>
                          {rec.confidenceScore}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                        {rec.expectedAlphaBps.toFixed(0)} bps
                      </td>
                      <td className={`px-6 py-4 whitespace-nowrap text-sm text-right ${
                        rec.edgeOverCostBps >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {rec.edgeOverCostBps.toFixed(0)} bps
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        <div className="max-w-xs">
                          <div className="text-xs">
                            1. {rec.driver1Name} ({rec.driver1Score.toFixed(2)})
                          </div>
                          <div className="text-xs">
                            2. {rec.driver2Name} ({rec.driver2Score.toFixed(2)})
                          </div>
                          <div className="text-xs">
                            3. {rec.driver3Name} ({rec.driver3Score.toFixed(2)})
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Expandable Explanation Section */}
              {recommendations && recommendations.length > 0 && (
                <div className="border-t border-gray-200 p-6 bg-gray-50">
                  <h3 className="font-semibold text-gray-900 mb-4">Detailed Explanations</h3>
                  <div className="space-y-4">
                    {recommendations.slice(0, 5).map((rec) => (
                      <div key={rec.id} className="bg-white p-4 rounded-lg border border-gray-200">
                        <div className="flex items-start justify-between mb-2">
                          <div>
                            <span className="font-semibold text-gray-900">#{rec.rank} {rec.symbol}</span>
                            <span className="ml-2 text-sm text-gray-600">
                              {rec.marketCapTier} • Liquidity Tier {rec.liquidityTier}
                            </span>
                          </div>
                        </div>
                        <p className="text-sm text-gray-700">{rec.explanation}</p>
                        {rec.constraintNotes && (
                          <p className="mt-2 text-xs text-orange-600">
                            <strong>Note:</strong> {rec.constraintNotes}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
      </div>
    </>
  );
}
