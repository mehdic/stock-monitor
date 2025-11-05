import { PerformanceContributorDTO } from '../../services/factorService';

interface ContributorsTableProps {
  contributors: PerformanceContributorDTO[];
  title: string;
  type: 'contributor' | 'detractor';
}

/**
 * Table showing top contributors or detractors to portfolio performance (T163, FR-014).
 */
export default function ContributorsTable({ contributors, title, type }: ContributorsTableProps) {
  if (contributors.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
        <p className="text-gray-500 text-center py-4">No data available</p>
      </div>
    );
  }

  const positiveColor = type === 'contributor' ? 'text-green-600' : 'text-red-600';
  const negativeColor = type === 'contributor' ? 'text-red-600' : 'text-green-600';

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead>
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Symbol
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Sector
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                P&L
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Return
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Weight
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {contributors.map((contributor) => (
              <tr key={contributor.symbol} className="hover:bg-gray-50">
                <td className="px-4 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {contributor.symbol}
                </td>
                <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-500">
                  {contributor.sector}
                </td>
                <td
                  className={`px-4 py-4 whitespace-nowrap text-sm text-right font-medium ${
                    contributor.pnl >= 0 ? positiveColor : negativeColor
                  }`}
                >
                  ${contributor.pnl.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </td>
                <td
                  className={`px-4 py-4 whitespace-nowrap text-sm text-right font-medium ${
                    contributor.pnlPct >= 0 ? positiveColor : negativeColor
                  }`}
                >
                  {contributor.pnlPct >= 0 ? '+' : ''}
                  {contributor.pnlPct.toFixed(2)}%
                </td>
                <td className="px-4 py-4 whitespace-nowrap text-sm text-right text-gray-500">
                  {contributor.weight.toFixed(1)}%
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
