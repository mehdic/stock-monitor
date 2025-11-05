import { useState } from 'react';

export interface Recommendation {
  id: string;
  symbol: string;
  action: 'BUY' | 'SELL' | 'HOLD';
  targetWeightPct: number;
  currentWeightPct: number;
  tradeSizePct: number;
  rationale: string;
  factorScores: {
    value?: number;
    momentum?: number;
    quality?: number;
    size?: number;
    volatility?: number;
  };
  constraintStatus: 'OK' | 'VIOLATED' | 'WARNING';
  violatedConstraints?: string[];
}

interface RecommendationCardProps {
  recommendation: Recommendation;
  onViewDetails?: (recommendation: Recommendation) => void;
}

export default function RecommendationCard({ recommendation, onViewDetails }: RecommendationCardProps) {
  const [expanded, setExpanded] = useState(false);

  const getActionColor = (action: string) => {
    switch (action) {
      case 'BUY':
        return 'bg-green-100 text-green-800 border-green-300';
      case 'SELL':
        return 'bg-red-100 text-red-800 border-red-300';
      case 'HOLD':
        return 'bg-gray-100 text-gray-800 border-gray-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  const getConstraintStatusColor = (status: string) => {
    switch (status) {
      case 'OK':
        return 'text-green-600';
      case 'WARNING':
        return 'text-yellow-600';
      case 'VIOLATED':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const formatPercent = (value: number): string => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
  };

  const formatFactorScore = (score?: number): string => {
    if (score === undefined || score === null) return 'N/A';
    return score.toFixed(2);
  };

  const getFactorScoreColor = (score?: number): string => {
    if (score === undefined || score === null) return 'text-gray-500';
    if (score >= 0.7) return 'text-green-600 font-semibold';
    if (score >= 0.4) return 'text-yellow-600';
    return 'text-red-600';
  };

  return (
    <div className="bg-white rounded-lg shadow-md border border-gray-200 hover:shadow-lg transition-shadow">
      {/* Card Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center space-x-3">
              <h3 className="text-xl font-bold text-gray-900">{recommendation.symbol}</h3>
              <span
                className={`px-3 py-1 rounded-full text-sm font-semibold border ${getActionColor(
                  recommendation.action
                )}`}
              >
                {recommendation.action}
              </span>
            </div>
            <p className="text-sm text-gray-600 mt-1">{recommendation.rationale}</p>
          </div>

          <button
            onClick={() => setExpanded(!expanded)}
            className="ml-4 text-blue-600 hover:text-blue-800 transition-colors"
            aria-label={expanded ? 'Collapse details' : 'Expand details'}
          >
            <svg
              className={`w-6 h-6 transform transition-transform ${expanded ? 'rotate-180' : ''}`}
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-4">
        {/* Weight and Trade Size */}
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div>
            <p className="text-xs text-gray-500 uppercase">Current Weight</p>
            <p className="text-lg font-semibold text-gray-900">{recommendation.currentWeightPct.toFixed(2)}%</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase">Target Weight</p>
            <p className="text-lg font-semibold text-gray-900">{recommendation.targetWeightPct.toFixed(2)}%</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase">Trade Size</p>
            <p
              className={`text-lg font-semibold ${
                recommendation.tradeSizePct >= 0 ? 'text-green-600' : 'text-red-600'
              }`}
            >
              {formatPercent(recommendation.tradeSizePct)}
            </p>
          </div>
        </div>

        {/* Constraint Status */}
        <div className="flex items-center justify-between mb-4">
          <span className="text-sm text-gray-600">Constraint Status:</span>
          <span className={`text-sm font-semibold ${getConstraintStatusColor(recommendation.constraintStatus)}`}>
            {recommendation.constraintStatus}
          </span>
        </div>

        {/* Violated Constraints Warning */}
        {recommendation.violatedConstraints && recommendation.violatedConstraints.length > 0 && (
          <div className="bg-yellow-50 border border-yellow-200 rounded p-3 mb-4">
            <p className="text-sm font-medium text-yellow-800">Constraint Violations:</p>
            <ul className="mt-1 list-disc list-inside text-sm text-yellow-700">
              {recommendation.violatedConstraints.map((constraint, idx) => (
                <li key={idx}>{constraint}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Expanded Details */}
        {expanded && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <h4 className="text-sm font-semibold text-gray-900 mb-3">Factor Scores</h4>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {Object.entries(recommendation.factorScores).map(([factor, score]) => (
                <div key={factor} className="bg-gray-50 rounded p-2">
                  <p className="text-xs text-gray-500 uppercase">{factor}</p>
                  <p className={`text-lg font-semibold ${getFactorScoreColor(score)}`}>
                    {formatFactorScore(score)}
                  </p>
                </div>
              ))}
            </div>

            {onViewDetails && (
              <button
                onClick={() => onViewDetails(recommendation)}
                className="mt-4 w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 transition-colors"
              >
                View Full Details
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
