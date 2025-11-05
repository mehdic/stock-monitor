interface ProgressBarProps {
  progress: number; // 0-100
  status?: 'running' | 'success' | 'error';
  message?: string;
  showPercentage?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export default function ProgressBar({
  progress,
  status = 'running',
  message,
  showPercentage = true,
  size = 'md',
}: ProgressBarProps) {
  // Clamp progress to 0-100
  const clampedProgress = Math.min(Math.max(progress, 0), 100);

  const getStatusColor = (status: 'running' | 'success' | 'error') => {
    switch (status) {
      case 'success':
        return 'bg-green-600';
      case 'error':
        return 'bg-red-600';
      case 'running':
      default:
        return 'bg-blue-600';
    }
  };

  const getBackgroundColor = (status: 'running' | 'success' | 'error') => {
    switch (status) {
      case 'success':
        return 'bg-green-100';
      case 'error':
        return 'bg-red-100';
      case 'running':
      default:
        return 'bg-gray-200';
    }
  };

  const getHeightClass = (size: 'sm' | 'md' | 'lg') => {
    switch (size) {
      case 'sm':
        return 'h-1';
      case 'lg':
        return 'h-4';
      case 'md':
      default:
        return 'h-2';
    }
  };

  return (
    <div className="w-full">
      {message && (
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-gray-700">{message}</span>
          {showPercentage && (
            <span className="text-sm font-medium text-gray-900">{clampedProgress}%</span>
          )}
        </div>
      )}
      <div className={`w-full rounded-full overflow-hidden ${getBackgroundColor(status)} ${getHeightClass(size)}`}>
        <div
          className={`${getHeightClass(size)} rounded-full transition-all duration-300 ease-out ${getStatusColor(status)}`}
          style={{ width: `${clampedProgress}%` }}
          role="progressbar"
          aria-valuenow={clampedProgress}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={message || `Progress: ${clampedProgress}%`}
        >
          {status === 'running' && clampedProgress > 0 && clampedProgress < 100 && (
            <div className="h-full w-full animate-pulse" />
          )}
        </div>
      </div>
    </div>
  );
}
