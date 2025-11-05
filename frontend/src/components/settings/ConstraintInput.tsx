import { ChangeEvent } from 'react';

interface ConstraintInputProps {
  label: string;
  name: string;
  value: number | undefined;
  onChange: (name: string, value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  unit?: string;
  tooltip?: string;
  error?: string;
}

/**
 * Constraint input component with tooltips (T143).
 */
export default function ConstraintInput({
  label,
  name,
  value,
  onChange,
  min = 0,
  max = 100,
  step = 0.1,
  unit = '%',
  tooltip,
  error,
}: ConstraintInputProps) {
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const newValue = parseFloat(e.target.value);
    if (!isNaN(newValue)) {
      onChange(name, newValue);
    }
  };

  return (
    <div className="mb-4">
      <div className="flex items-center justify-between mb-2">
        <label htmlFor={name} className="block text-sm font-medium text-gray-700">
          {label}
          {tooltip && (
            <span className="ml-2 text-gray-400 cursor-help" title={tooltip}>
              <svg className="inline-block h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                  clipRule="evenodd"
                />
              </svg>
            </span>
          )}
        </label>
        <span className="text-sm text-gray-600">
          {value?.toFixed(1)}
          {unit}
        </span>
      </div>

      <div className="flex items-center space-x-3">
        <input
          type="range"
          id={name}
          name={name}
          value={value ?? min}
          onChange={handleChange}
          min={min}
          max={max}
          step={step}
          className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider"
        />
        <input
          type="number"
          value={value ?? ''}
          onChange={handleChange}
          min={min}
          max={max}
          step={step}
          className={`w-20 px-2 py-1 text-sm border rounded ${
            error ? 'border-red-500' : 'border-gray-300'
          } focus:outline-none focus:ring-2 focus:ring-blue-500`}
        />
      </div>

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}

      <div className="flex justify-between mt-1 text-xs text-gray-500">
        <span>
          {min}
          {unit}
        </span>
        <span>
          {max}
          {unit}
        </span>
      </div>
    </div>
  );
}
