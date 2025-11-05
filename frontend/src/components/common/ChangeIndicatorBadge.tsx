interface ChangeIndicatorBadgeProps {
  changeIndicator: string;
  weightChange?: number; // Optional weight change value to display
  size?: 'sm' | 'md' | 'lg';
  showIcon?: boolean;
}

export default function ChangeIndicatorBadge({
  changeIndicator,
  weightChange,
  size = 'md',
  showIcon = true,
}: ChangeIndicatorBadgeProps) {
  const getIndicatorStyles = (indicator: string) => {
    const normalizedIndicator = indicator.toUpperCase();

    switch (normalizedIndicator) {
      case 'NEW':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'INCREASED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'DECREASED':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'UNCHANGED':
        return 'bg-gray-100 text-gray-600 border-gray-200';
      case 'REMOVED':
      case 'REMOVE':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'MODIFY':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default:
        return 'bg-gray-100 text-gray-600 border-gray-200';
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

  const getIndicatorIcon = (indicator: string) => {
    if (!showIcon) return null;

    const normalizedIndicator = indicator.toUpperCase();
    const iconClasses = size === 'sm' ? 'h-3 w-3 mr-1' : 'h-4 w-4 mr-1.5';

    switch (normalizedIndicator) {
      case 'NEW':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'INCREASED':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M5.293 7.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L6.707 7.707a1 1 0 01-1.414 0z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'DECREASED':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M14.707 12.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 14.586V3a1 1 0 012 0v11.586l2.293-2.293a1 1 0 011.414 0z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'UNCHANGED':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M4 10a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'REMOVED':
      case 'REMOVE':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"
              clipRule="evenodd"
            />
          </svg>
        );
      case 'MODIFY':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20">
            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
          </svg>
        );
      default:
        return null;
    }
  };

  const formatChangeText = (indicator: string) => {
    return indicator
      .replace(/_/g, ' ')
      .split(' ')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  const formatWeightChange = (weightChange: number) => {
    const sign = weightChange >= 0 ? '+' : '';
    return `${sign}${weightChange.toFixed(2)}%`;
  };

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${getIndicatorStyles(
        changeIndicator
      )} ${getSizeClasses(size)}`}
    >
      {getIndicatorIcon(changeIndicator)}
      <span>
        {formatChangeText(changeIndicator)}
        {weightChange !== undefined && ` (${formatWeightChange(weightChange)})`}
      </span>
    </span>
  );
}
