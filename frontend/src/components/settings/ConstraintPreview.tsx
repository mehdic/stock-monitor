import { ConstraintPreviewDTO } from '../../services/constraintService';

interface ConstraintPreviewProps {
  preview: ConstraintPreviewDTO | null;
  isLoading: boolean;
}

/**
 * Constraint preview panel component (T144).
 *
 * Displays impact estimates with accuracy ranges.
 */
export default function ConstraintPreview({ preview, isLoading }: ConstraintPreviewProps) {
  if (isLoading) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <div className="flex items-center justify-center">
          <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full"></div>
          <span className="ml-3 text-blue-900">Calculating preview...</span>
        </div>
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
        <p className="text-gray-600 text-center">
          Modify constraints and click "Preview Impact" to see estimated changes
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">Impact Preview</h3>

      {/* Changes Summary */}
      <div className="bg-blue-50 border-l-4 border-blue-500 p-4">
        <p className="text-sm text-blue-900 font-medium">Changes:</p>
        <p className="text-sm text-blue-800">{preview.constraintChangesSummary}</p>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Expected Picks</p>
          <p className="text-2xl font-bold text-gray-900">{preview.expectedPickCount}</p>
          <p className="text-xs text-gray-500 mt-1">{preview.expectedPickCountRange}</p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600 mb-1">Expected Turnover</p>
          <p className="text-2xl font-bold text-gray-900">{preview.expectedTurnoverPct.toFixed(1)}%</p>
          <p className="text-xs text-gray-500 mt-1">{preview.expectedTurnoverRange}</p>
        </div>
      </div>

      {/* Affected Positions */}
      {preview.affectedPositionsCount > 0 && (
        <div className="border-t border-gray-200 pt-4">
          <p className="text-sm font-medium text-gray-900 mb-2">
            Affected Positions: {preview.affectedPositionsCount}
          </p>

          <div className="grid grid-cols-2 gap-4">
            {preview.droppedSymbols.length > 0 && (
              <div>
                <p className="text-xs font-medium text-red-700 mb-1">
                  Dropped ({preview.droppedSymbols.length})
                </p>
                <div className="flex flex-wrap gap-1">
                  {preview.droppedSymbols.slice(0, 10).map((symbol) => (
                    <span
                      key={symbol}
                      className="px-2 py-0.5 text-xs bg-red-100 text-red-800 rounded"
                    >
                      {symbol}
                    </span>
                  ))}
                  {preview.droppedSymbols.length > 10 && (
                    <span className="px-2 py-0.5 text-xs text-red-600">
                      +{preview.droppedSymbols.length - 10} more
                    </span>
                  )}
                </div>
              </div>
            )}

            {preview.addedSymbols.length > 0 && (
              <div>
                <p className="text-xs font-medium text-green-700 mb-1">
                  Added ({preview.addedSymbols.length})
                </p>
                <div className="flex flex-wrap gap-1">
                  {preview.addedSymbols.slice(0, 10).map((symbol) => (
                    <span
                      key={symbol}
                      className="px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded"
                    >
                      {symbol}
                    </span>
                  ))}
                  {preview.addedSymbols.length > 10 && (
                    <span className="px-2 py-0.5 text-xs text-green-600">
                      +{preview.addedSymbols.length - 10} more
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Warnings */}
      {preview.warnings && preview.warnings.length > 0 && (
        <div className="border-t border-gray-200 pt-4">
          {preview.warnings.map((warning, index) => (
            <div key={index} className="bg-yellow-50 border-l-4 border-yellow-400 p-3 mb-2">
              <p className="text-sm text-yellow-800">{warning}</p>
            </div>
          ))}
        </div>
      )}

      {/* Accuracy Note */}
      <div className="border-t border-gray-200 pt-4">
        <p className="text-xs text-gray-500 italic">{preview.accuracyNote}</p>
      </div>
    </div>
  );
}
