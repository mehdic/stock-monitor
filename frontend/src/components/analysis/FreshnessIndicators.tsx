import { useQuery } from '@tanstack/react-query';
import { factorService, DataSourceHealthDTO } from '../../services/factorService';

/**
 * Data freshness indicators showing status of data sources (T168, FR-037, FR-038).
 */
export default function FreshnessIndicators() {
  const { data: dataSources, isLoading } = useQuery({
    queryKey: ['dataSources'],
    queryFn: () => factorService.getAllDataSources(),
    refetchInterval: 60000, // Refresh every minute
  });

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow p-4">
        <h4 className="text-sm font-semibold text-gray-700 mb-2">Data Freshness</h4>
        <p className="text-xs text-gray-500">Loading...</p>
      </div>
    );
  }

  if (!dataSources || dataSources.length === 0) {
    return null;
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return 'text-green-600 bg-green-50';
      case 'STALE':
        return 'text-yellow-600 bg-yellow-50';
      case 'UNAVAILABLE':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return '✓';
      case 'STALE':
        return '⚠';
      case 'UNAVAILABLE':
        return '✗';
      default:
        return '?';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-4">
      <h4 className="text-sm font-semibold text-gray-700 mb-3">Data Freshness</h4>
      <div className="space-y-2">
        {dataSources.map((source: DataSourceHealthDTO) => (
          <div
            key={source.id}
            className="flex items-center justify-between py-2 px-3 rounded-lg border border-gray-200"
          >
            <div className="flex items-center space-x-2">
              <span
                className={`inline-flex items-center justify-center w-5 h-5 rounded-full text-xs font-bold ${getStatusColor(
                  source.status
                )}`}
              >
                {getStatusIcon(source.status)}
              </span>
              <span className="text-sm font-medium text-gray-700">{source.name}</span>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-500">
                {source.minutesSinceUpdate !== undefined
                  ? `${source.minutesSinceUpdate}m ago`
                  : 'No data'}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Warning if any source is stale/unavailable */}
      {dataSources.some((s: DataSourceHealthDTO) => s.status !== 'HEALTHY') && (
        <div className="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded">
          <p className="text-xs text-yellow-800">
            Some data sources are stale. Recommendations may be affected.
          </p>
        </div>
      )}
    </div>
  );
}
