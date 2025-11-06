import { Recommendation } from './RecommendationCard';

interface ExplanationPanelProps {
  recommendation: Recommendation;
  onClose?: () => void;
}

export default function ExplanationPanel({ recommendation, onClose }: ExplanationPanelProps) {
  const getActionExplanation = (action: string): string => {
    switch (action) {
      case 'BUY':
        return 'Increase position size to achieve target weight. This stock has strong factor scores and fits within portfolio constraints.';
      case 'SELL':
        return 'Reduce position size to achieve target weight. This stock has weak factor scores or violates portfolio constraints.';
      case 'HOLD':
        return 'Maintain current position. This stock is appropriately weighted and meets portfolio objectives.';
      default:
        return 'No action required.';
    }
  };

  const getFactorExplanation = (factor: string): string => {
    switch (factor.toLowerCase()) {
      case 'value':
        return 'Value score measures how cheap the stock is relative to fundamentals (P/E, P/B ratios). Higher scores indicate better value.';
      case 'momentum':
        return 'Momentum score measures recent price trends and relative strength. Higher scores indicate positive price momentum.';
      case 'quality':
        return 'Quality score measures financial health (ROE, debt ratios, earnings stability). Higher scores indicate higher quality businesses.';
      case 'size':
        return 'Size score measures market capitalization. Higher scores favor larger, more liquid companies.';
      case 'volatility':
        return 'Volatility score measures price stability and risk. Higher scores indicate lower volatility (more stable stocks).';
      default:
        return 'Factor score measures specific investment characteristics.';
    }
  };

  const getFactorScoreInterpretation = (score?: number): string => {
    if (score === undefined || score === null) return 'No data available';
    if (score >= 0.7) return 'Strong - Above target';
    if (score >= 0.4) return 'Moderate - Near target';
    return 'Weak - Below target';
  };

  const getConstraintExplanation = (status: string): string => {
    switch (status) {
      case 'OK':
        return 'This recommendation meets all portfolio constraints including position limits, sector exposure, and turnover restrictions.';
      case 'WARNING':
        return 'This recommendation is approaching constraint limits. Review carefully before executing.';
      case 'VIOLATED':
        return 'This recommendation violates one or more portfolio constraints. It may not be executable without adjustments.';
      default:
        return 'Constraint status unknown.';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg border border-gray-200">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-900">Recommendation Explanation: {recommendation.symbol}</h2>
        {onClose && (
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            aria-label="Close"
          >
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
        )}
      </div>

      {/* Body */}
      <div className="p-6 space-y-6">
        {/* Action Explanation */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">
            Recommended Action: <span className="text-blue-600">{recommendation.action}</span>
          </h3>
          <p className="text-gray-700">{getActionExplanation(recommendation.action)}</p>
          <div className="mt-3 bg-blue-50 border border-blue-200 rounded p-4">
            <p className="text-sm text-blue-800">
              <span className="font-semibold">Rationale:</span> {recommendation.rationale}
            </p>
          </div>
        </section>

        {/* Weight Analysis */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Weight Analysis</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-gray-50 rounded p-4">
              <p className="text-sm text-gray-600">Current Weight</p>
              <p className="text-2xl font-bold text-gray-900">{recommendation.currentWeightPct.toFixed(2)}%</p>
            </div>
            <div className="bg-blue-50 rounded p-4">
              <p className="text-sm text-blue-600">Target Weight</p>
              <p className="text-2xl font-bold text-blue-900">{recommendation.targetWeightPct.toFixed(2)}%</p>
            </div>
            <div className="bg-green-50 rounded p-4">
              <p className="text-sm text-green-600">Trade Size</p>
              <p className="text-2xl font-bold text-green-900">
                {recommendation.tradeSizePct >= 0 ? '+' : ''}
                {recommendation.tradeSizePct.toFixed(2)}%
              </p>
            </div>
          </div>
          <p className="text-sm text-gray-600 mt-3">
            To achieve the target portfolio composition, adjust this holding by{' '}
            <span className="font-semibold">{Math.abs(recommendation.tradeSizePct).toFixed(2)}%</span> of your total
            portfolio value.
          </p>
        </section>

        {/* Factor Scores Breakdown */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Factor Scores Breakdown</h3>
          <p className="text-sm text-gray-600 mb-4">
            Factor scores range from 0.0 (weak) to 1.0 (strong). These scores drive the optimization algorithm to
            select stocks with desirable characteristics.
          </p>
          <div className="space-y-3">
            {Object.entries(recommendation.factorScores).map(([factor, score]) => (
              <div key={factor} className="border border-gray-200 rounded p-4">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="font-semibold text-gray-900 capitalize">{factor}</h4>
                  <div className="text-right">
                    <p className="text-xl font-bold text-gray-900">{score?.toFixed(2) || 'N/A'}</p>
                    <p className="text-xs text-gray-500">{getFactorScoreInterpretation(score)}</p>
                  </div>
                </div>
                <p className="text-sm text-gray-600">{getFactorExplanation(factor)}</p>
                {score !== undefined && score !== null && (
                  <div className="mt-2 bg-gray-100 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        score >= 0.7 ? 'bg-green-500' : score >= 0.4 ? 'bg-yellow-500' : 'bg-red-500'
                      }`}
                      style={{ width: `${score * 100}%` }}
                    ></div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* Constraint Status */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Constraint Compliance</h3>
          <div
            className={`border rounded p-4 ${
              recommendation.constraintStatus === 'OK'
                ? 'border-green-200 bg-green-50'
                : recommendation.constraintStatus === 'WARNING'
                ? 'border-yellow-200 bg-yellow-50'
                : 'border-red-200 bg-red-50'
            }`}
          >
            <div className="flex items-center justify-between mb-2">
              <span className="font-semibold text-gray-900">Status:</span>
              <span
                className={`font-bold ${
                  recommendation.constraintStatus === 'OK'
                    ? 'text-green-700'
                    : recommendation.constraintStatus === 'WARNING'
                    ? 'text-yellow-700'
                    : 'text-red-700'
                }`}
              >
                {recommendation.constraintStatus}
              </span>
            </div>
            <p className="text-sm text-gray-700">{getConstraintExplanation(recommendation.constraintStatus)}</p>
            {recommendation.violatedConstraints && recommendation.violatedConstraints.length > 0 && (
              <div className="mt-3">
                <p className="text-sm font-semibold text-gray-900 mb-1">Violations:</p>
                <ul className="list-disc list-inside text-sm text-gray-700">
                  {recommendation.violatedConstraints.map((constraint, idx) => (
                    <li key={idx}>{constraint}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </section>

        {/* Disclaimer */}
        <section className="bg-gray-50 border border-gray-200 rounded p-4">
          <p className="text-xs text-gray-600">
            <span className="font-semibold">Disclaimer:</span> This recommendation is generated by quantitative models
            and is for informational purposes only. It does not constitute financial advice. Past performance does not
            guarantee future results. Please consult with a qualified financial advisor before making investment
            decisions.
          </p>
        </section>
      </div>
    </div>
  );
}
