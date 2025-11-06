import { FactorScoreDTO } from '../../services/factorService';
import FactorTooltip from './FactorTooltip';

interface FactorHeatmapProps {
  factorScores: FactorScoreDTO[];
}

/**
 * Heatmap visualization of factor scores for portfolio holdings (T166, FR-034, FR-036).
 */
export default function FactorHeatmap({ factorScores }: FactorHeatmapProps) {
  const factors = ['value', 'momentum', 'quality', 'revisions'];

  // Color scale for z-scores: red (-2) -> yellow (0) -> green (+2)
  const getColorFromZScore = (zScore: number): string => {
    if (zScore >= 2) return 'bg-green-600';
    if (zScore >= 1) return 'bg-green-400';
    if (zScore >= 0.5) return 'bg-green-200';
    if (zScore >= -0.5) return 'bg-yellow-100';
    if (zScore >= -1) return 'bg-orange-200';
    if (zScore >= -2) return 'bg-red-400';
    return 'bg-red-600';
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Factor Analysis Heatmap</h3>
      <p className="text-sm text-gray-600 mb-6">
        Sector-normalized factor scores (z-scores). Green = above sector average, Red = below
        average.
      </p>

      {/* Heatmap Grid */}
      <div className="overflow-x-auto">
        <table className="min-w-full border border-gray-200">
          <thead>
            <tr className="bg-gray-50">
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase border-b">
                Symbol
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase border-b">
                Sector
              </th>
              {factors.map((factor) => (
                <th
                  key={factor}
                  className="px-4 py-3 text-center text-xs font-medium text-gray-700 uppercase border-b"
                >
                  {factor}
                </th>
              ))}
              <th className="px-4 py-3 text-center text-xs font-medium text-gray-700 uppercase border-b">
                Composite
              </th>
            </tr>
          </thead>
          <tbody>
            {factorScores.map((score) => (
              <tr key={score.symbol} className="hover:bg-gray-50 border-b">
                <td className="px-4 py-3 text-sm font-medium text-gray-900">{score.symbol}</td>
                <td className="px-4 py-3 text-sm text-gray-600">{score.sector}</td>
                <td className={`px-4 py-3 text-center ${getColorFromZScore(score.value)}`}>
                  <FactorTooltip
                    symbol={score.symbol}
                    factorName="Value"
                    zScore={score.value}
                    percentile={score.valuePercentile}
                  >
                    <span className="text-sm font-medium">{score.value.toFixed(2)}</span>
                  </FactorTooltip>
                </td>
                <td className={`px-4 py-3 text-center ${getColorFromZScore(score.momentum)}`}>
                  <FactorTooltip
                    symbol={score.symbol}
                    factorName="Momentum"
                    zScore={score.momentum}
                    percentile={score.momentumPercentile}
                  >
                    <span className="text-sm font-medium">{score.momentum.toFixed(2)}</span>
                  </FactorTooltip>
                </td>
                <td className={`px-4 py-3 text-center ${getColorFromZScore(score.quality)}`}>
                  <FactorTooltip
                    symbol={score.symbol}
                    factorName="Quality"
                    zScore={score.quality}
                    percentile={score.qualityPercentile}
                  >
                    <span className="text-sm font-medium">{score.quality.toFixed(2)}</span>
                  </FactorTooltip>
                </td>
                <td className={`px-4 py-3 text-center ${getColorFromZScore(score.revisions)}`}>
                  <FactorTooltip
                    symbol={score.symbol}
                    factorName="Revisions"
                    zScore={score.revisions}
                    percentile={score.revisionsPercentile}
                  >
                    <span className="text-sm font-medium">{score.revisions.toFixed(2)}</span>
                  </FactorTooltip>
                </td>
                <td className={`px-4 py-3 text-center font-bold ${getColorFromZScore(score.composite)}`}>
                  {score.composite.toFixed(2)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Legend */}
      <div className="mt-6 flex items-center justify-center space-x-4">
        <span className="text-xs text-gray-600">Z-Score:</span>
        <div className="flex items-center space-x-2">
          <div className="w-8 h-4 bg-red-600 rounded"></div>
          <span className="text-xs text-gray-600">{'< -2'}</span>
        </div>
        <div className="flex items-center space-x-2">
          <div className="w-8 h-4 bg-yellow-100 rounded"></div>
          <span className="text-xs text-gray-600">~0</span>
        </div>
        <div className="flex items-center space-x-2">
          <div className="w-8 h-4 bg-green-600 rounded"></div>
          <span className="text-xs text-gray-600">{'>  +2'}</span>
        </div>
      </div>
    </div>
  );
}
