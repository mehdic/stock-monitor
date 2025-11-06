import { ReactNode } from 'react';

interface FactorTooltipProps {
  symbol: string;
  factorName: string;
  zScore: number;
  percentile?: number;
  children: ReactNode;
}

/**
 * Tooltip component explaining factor scores (T167, FR-036).
 */
export default function FactorTooltip({
  symbol,
  factorName,
  zScore,
  percentile,
  children,
}: FactorTooltipProps) {
  const factorExplanations: Record<string, string> = {
    Value: 'Valuation metrics (P/E, P/B, FCF yield). Lower valuations score higher.',
    Momentum: 'Price momentum (3M, 6M, 12M returns). Stronger momentum scores higher.',
    Quality: 'Profitability metrics (ROE, margins, leverage). Higher quality scores higher.',
    Revisions: 'Analyst estimate changes. Positive revisions score higher.',
  };

  const interpretation =
    zScore >= 1
      ? 'Above sector average'
      : zScore >= -1
      ? 'Near sector average'
      : 'Below sector average';

  return (
    <div className="group relative inline-block">
      {children}
      <div className="invisible group-hover:visible absolute z-10 w-64 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-lg -top-2 left-full ml-2">
        <div className="font-semibold mb-1">
          {symbol} - {factorName}
        </div>
        <div className="space-y-1">
          <p>{factorExplanations[factorName]}</p>
          <p className="mt-2">
            <span className="font-medium">Z-Score:</span> {zScore.toFixed(2)}
          </p>
          {percentile !== undefined && (
            <p>
              <span className="font-medium">Percentile:</span> {percentile}th
            </p>
          )}
          <p className="mt-1 text-yellow-300">{interpretation}</p>
        </div>
        {/* Arrow */}
        <div className="absolute top-2 -left-1 w-2 h-2 bg-gray-900 transform rotate-45"></div>
      </div>
    </div>
  );
}
