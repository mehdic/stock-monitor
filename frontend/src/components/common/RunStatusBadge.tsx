interface RunStatusBadgeProps {
  status: string;
  size?: 'sm' | 'md' | 'lg';
}

export default function RunStatusBadge({ status, size = 'md' }: RunStatusBadgeProps) {
  const getStatusStyles = (status: string) => {
    const normalizedStatus = status.toUpperCase();

    switch (normalizedStatus) {
      case 'RUNNING':
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'COMPLETED':
      case 'FINALIZED':
      case 'SUCCESS':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'FAILED':
      case 'ERROR':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'PENDING':
      case 'SCHEDULED':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'CANCELLED':
      case 'CANCELED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getSizeClasses = (size: 'sm' | 'md' | 'lg') => {
    switch (size) {
      case 'sm':
        return 'text-xs px-2 py-0.5';
      case 'lg':
        return 'text-base px-4 py-2';
      case 'md':
      default:
        return 'text-sm px-3 py-1';
    }
  };

  const getStatusIcon = (status: string) => {
    const normalizedStatus = status.toUpperCase();

    switch (normalizedStatus) {
      case 'RUNNING':
      case 'IN_PROGRESS':
        return (
          <svg
            className="animate-spin h-3 w-3 mr-1"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            ></circle>
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            ></path>
          </svg>
        );
      case 'COMPLETED':
      case 'FINALIZED':
      case 'SUCCESS':
        return (
          <svg className="h-3 w-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'FAILED':
      case 'ERROR':
        return (
          <svg className="h-3 w-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'PENDING':
      case 'SCHEDULED':
        return (
          <svg className="h-3 w-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
              clipRule="evenodd"
            />
          </svg>
        );
      default:
        return null;
    }
  };

  const formatStatusText = (status: string) => {
    return status
      .replace(/_/g, ' ')
      .split(' ')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${getStatusStyles(
        status
      )} ${getSizeClasses(size)}`}
    >
      {getStatusIcon(status)}
      {formatStatusText(status)}
    </span>
  );
}
