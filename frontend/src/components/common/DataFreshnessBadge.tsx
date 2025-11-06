interface DataFreshnessBadgeProps {
  asOfDate: string;
  stalenessHours?: number;
  showDetails?: boolean;
}

export default function DataFreshnessBadge({ asOfDate, stalenessHours, showDetails = false }: DataFreshnessBadgeProps) {
  const getFreshnessStatus = (hours?: number): 'fresh' | 'stale' | 'very-stale' => {
    if (hours === undefined) return 'fresh';
    if (hours <= 24) return 'fresh';
    if (hours <= 72) return 'stale';
    return 'very-stale';
  };

  const getFreshnessColor = (status: string): string => {
    switch (status) {
      case 'fresh':
        return 'bg-green-100 text-green-800 border-green-300';
      case 'stale':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'very-stale':
        return 'bg-red-100 text-red-800 border-red-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  const getFreshnessLabel = (status: string): string => {
    switch (status) {
      case 'fresh':
        return 'Fresh';
      case 'stale':
        return 'Stale';
      case 'very-stale':
        return 'Very Stale';
      default:
        return 'Unknown';
    }
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatStaleness = (hours?: number): string => {
    if (hours === undefined) return 'Unknown';
    if (hours < 1) return 'Less than 1 hour ago';
    if (hours < 24) return `${Math.floor(hours)} hours ago`;
    const days = Math.floor(hours / 24);
    return `${days} day${days > 1 ? 's' : ''} ago`;
  };

  const status = getFreshnessStatus(stalenessHours);

  return (
    <div className={`inline-flex items-center px-3 py-1 rounded-full border text-sm font-medium ${getFreshnessColor(status)}`}>
      {/* Status Indicator Dot */}
      <span
        className={`h-2 w-2 rounded-full mr-2 ${
          status === 'fresh' ? 'bg-green-600' : status === 'stale' ? 'bg-yellow-600' : 'bg-red-600'
        }`}
      ></span>

      {/* Badge Content */}
      <div className="flex items-center space-x-2">
        <span>{getFreshnessLabel(status)}</span>
        {showDetails && (
          <>
            <span>•</span>
            <span className="text-xs">
              {formatDate(asOfDate)}
              {stalenessHours !== undefined && ` (${formatStaleness(stalenessHours)})`}
            </span>
          </>
        )}
      </div>

      {/* Info Icon with Tooltip */}
      {!showDetails && (
        <span className="ml-1 cursor-help" title={`As of ${formatDate(asOfDate)}\n${formatStaleness(stalenessHours)}`}>
          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clipRule="evenodd"
            />
          </svg>
        </span>
      )}
    </div>
  );
}
