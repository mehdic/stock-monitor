interface ExclusionDTO {
  symbol: string;
  companyName: string;
  exclusionReasonCode: string;
  explanation: string;
  runDate: string;
}

interface ExclusionDetailModalProps {
  exclusion: ExclusionDTO | null;
  onClose: () => void;
}

/**
 * Modal showing detailed exclusion reason (T201).
 */
export default function ExclusionDetailModal({ exclusion, onClose }: ExclusionDetailModalProps) {
  if (!exclusion) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Why was {exclusion.symbol} excluded?
        </h3>

        <div className="space-y-4">
          <div>
            <span className="text-sm font-medium text-gray-700">Company:</span>
            <p className="text-sm text-gray-900">{exclusion.companyName}</p>
          </div>

          <div>
            <span className="text-sm font-medium text-gray-700">Reason Code:</span>
            <p className="text-sm text-gray-900">{exclusion.exclusionReasonCode}</p>
          </div>

          <div>
            <span className="text-sm font-medium text-gray-700">Explanation:</span>
            <p className="text-sm text-gray-900">{exclusion.explanation}</p>
          </div>

          <div>
            <span className="text-sm font-medium text-gray-700">Run Date:</span>
            <p className="text-sm text-gray-900">
              {new Date(exclusion.runDate).toLocaleDateString()}
            </p>
          </div>
        </div>

        <button
          onClick={onClose}
          className="mt-6 w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Close
        </button>
      </div>
    </div>
  );
}
