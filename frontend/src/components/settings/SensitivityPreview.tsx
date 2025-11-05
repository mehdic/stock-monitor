import { SensitivityPreviewDTO } from '../../services/backtestService';

interface SensitivityPreviewProps {
  preview: SensitivityPreviewDTO | null;
  isLoading: boolean;
  onApply: () => void;
}

/**
 * Sensitivity preview component showing impact of constraint changes (T188-T189, FR-054, FR-055).
 */
export default function SensitivityPreview({
  preview,
  isLoading,
  onApply,
}: SensitivityPreviewProps) {
  if (isLoading) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center justify-center">
          <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full"></div>
          <span className="ml-3 text-blue-900">Analyzing sensitivity...</span>
        </div>
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <p className="text-gray-600 text-center">
          Adjust constraints to see sensitivity analysis
        </p>
      </div>
    );
  }

  const getSensitivityColor = (score: number) => {
    if (score >= 7) return 'text-red-600 bg-red-50';
    if (score >= 4) return 'text-yellow-600 bg-yellow-50';
    return 'text-green-600 bg-green-50';
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">Sensitivity Analysis</h3>

      {/* Constraint Change */}
      <div className="bg-blue-50 border-l-4 border-blue-500 p-4">
        <p className="text-sm text-blue-900 font-medium">
          {preview.constraintName}: {preview.originalValue} → {preview.newValue}
        </p>
      </div>

      {/* Impact Metrics */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Holdings Change</p>
          <p className="text-2xl font-bold text-gray-900">
            {preview.expectedHoldingsDelta >= 0 ? '+' : ''}
            {preview.expectedHoldingsDelta}
          </p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Turnover Change</p>
          <p className="text-2xl font-bold text-gray-900">
            {preview.expectedTurnoverDelta >= 0 ? '+' : ''}
            {preview.expectedTurnoverDelta.toFixed(1)}%
          </p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Return Impact</p>
          <p className={`text-2xl font-bold ${preview.expectedReturnDelta >= 0 ? 'text-green-600' : 'text-red-600'}`}>
            {preview.expectedReturnDelta >= 0 ? '+' : ''}
            {preview.expectedReturnDelta.toFixed(2)}%
          </p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Risk Impact</p>
          <p className="text-2xl font-bold text-gray-900">
            {preview.expectedRiskDelta >= 0 ? '+' : ''}
            {preview.expectedRiskDelta.toFixed(2)}%
          </p>
        </div>
      </div>

      {/* Sensitivity Score */}
      <div className="border-t border-gray-200 pt-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-700">Sensitivity Score</span>
          <span
            className={`px-3 py-1 rounded-full text-sm font-bold ${getSensitivityColor(
              preview.sensitivityScore
            )}`}
          >
            {preview.sensitivityScore}/10
          </span>
        </div>
        <p className="text-sm text-gray-600">{preview.impactSummary}</p>
      </div>

      {/* Recommendation */}
      <div className="bg-green-50 border border-green-200 rounded p-4">
        <p className="text-sm font-medium text-green-900 mb-1">Recommendation</p>
        <p className="text-sm text-green-800">{preview.recommendation}</p>
      </div>

      {/* Apply Button (T189) */}
      <button
        onClick={onApply}
        className="w-full px-4 py-2 bg-green-600 text-white font-medium rounded-lg hover:bg-green-700"
      >
        Apply to Constraints
      </button>
    </div>
  );
}
