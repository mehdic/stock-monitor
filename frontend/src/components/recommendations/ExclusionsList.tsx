import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../services/api';

interface ExclusionDTO {
  symbol: string;
  companyName: string;
  exclusionReasonCode: string;
  explanation: string;
  runDate: string;
}

interface ExclusionsListProps {
  runId: string;
  onShowDetail: (exclusion: ExclusionDTO) => void;
}

/**
 * List of excluded stocks with reasons (T200, T202, FR-031, FR-032).
 */
export default function ExclusionsList({ runId, onShowDetail }: ExclusionsListProps) {
  const { data: exclusions, isLoading } = useQuery({
    queryKey: ['exclusions', runId],
    queryFn: async () => {
      const response = await apiClient.get<ExclusionDTO[]>(`/runs/${runId}/exclusions`);
      return response.data;
    },
  });

  const handleExport = async () => {
    const response = await apiClient.get(`/runs/${runId}/exclusions/export`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `exclusions-${runId}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  if (isLoading) {
    return <div>Loading exclusions...</div>;
  }

  if (!exclusions || exclusions.length === 0) {
    return <div className="text-gray-500">No exclusions for this run</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold text-gray-900">
          Excluded Stocks ({exclusions.length})
        </h3>
        <button
          onClick={handleExport}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700"
        >
          Export to CSV
        </button>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Symbol
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Company
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Reason
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {exclusions.map((exclusion) => (
              <tr key={exclusion.symbol} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {exclusion.symbol}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {exclusion.companyName}
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {exclusion.explanation}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                  <button
                    onClick={() => onShowDetail(exclusion)}
                    className="text-blue-600 hover:text-blue-800"
                  >
                    Why not this one?
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
