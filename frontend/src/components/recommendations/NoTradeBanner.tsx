/**
 * Banner showing "No trade" guidance when expected edge doesn't cover costs (T203, FR-026, FR-069).
 */
export default function NoTradeBanner() {
  return (
    <div className="bg-yellow-50 border-l-4 border-yellow-400 p-6 rounded-lg shadow">
      <div className="flex">
        <div className="flex-shrink-0">
          <svg
            className="h-6 w-6 text-yellow-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>
        <div className="ml-4">
          <h3 className="text-lg font-semibold text-yellow-900">No Trade Recommended</h3>
          <div className="mt-2 text-sm text-yellow-800">
            <p>
              The expected advantage from rebalancing does not exceed transaction costs by a safe
              margin (1.5x).
            </p>
            <p className="mt-2">
              <strong>Recommendation:</strong> Hold current positions. The cost of trading would
              likely outweigh the benefit of the recommended changes.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
