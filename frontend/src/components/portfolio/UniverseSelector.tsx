import { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';

export interface Universe {
  id: string;
  name: string;
  description: string;
  constituentCount: number;
  marketCapMin?: number;
  marketCapMax?: number;
  lastUpdated: string;
}

interface UniverseSelectorProps {
  selectedUniverseId?: string;
  onSelect: (universeId: string) => void;
  disabled?: boolean;
}

export default function UniverseSelector({ selectedUniverseId, onSelect, disabled = false }: UniverseSelectorProps) {
  const [universes, setUniverses] = useState<Universe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchUniverses();
  }, []);

  const fetchUniverses = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiClient.get<Universe[]>('/api/universes');
      setUniverses(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load universes');
      console.error('Failed to fetch universes:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatMarketCap = (value?: number): string => {
    if (!value) return 'N/A';
    if (value >= 1_000_000_000) {
      return `$${(value / 1_000_000_000).toFixed(1)}B`;
    }
    if (value >= 1_000_000) {
      return `$${(value / 1_000_000).toFixed(1)}M`;
    }
    return `$${value.toLocaleString()}`;
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading universes...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        <p className="font-medium">Error Loading Universes</p>
        <p className="mt-1">{error}</p>
        <button
          onClick={fetchUniverses}
          className="mt-2 text-sm text-red-800 hover:text-red-900 underline"
        >
          Try Again
        </button>
      </div>
    );
  }

  if (universes.length === 0) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded">
        <p className="font-medium">No Universes Available</p>
        <p className="mt-1">Please contact support to set up investment universes.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-medium text-gray-900">Select Investment Universe</h3>
      <p className="text-sm text-gray-600">
        Choose the stock universe for portfolio optimization. This determines which stocks will be analyzed
        for recommendations.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {universes.map((universe) => {
          const isSelected = selectedUniverseId === universe.id;

          return (
            <button
              key={universe.id}
              onClick={() => !disabled && onSelect(universe.id)}
              disabled={disabled}
              className={`text-left p-4 border rounded-lg transition-all ${
                isSelected
                  ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-500'
                  : 'border-gray-300 bg-white hover:border-blue-300 hover:shadow-md'
              } ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h4 className="font-semibold text-gray-900">{universe.name}</h4>
                  <p className="text-sm text-gray-600 mt-1">{universe.description}</p>
                </div>
                {isSelected && (
                  <svg
                    className="h-6 w-6 text-blue-600 flex-shrink-0"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                      clipRule="evenodd"
                    />
                  </svg>
                )}
              </div>

              <div className="mt-3 space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Constituents:</span>
                  <span className="font-medium text-gray-900">{universe.constituentCount}</span>
                </div>

                {(universe.marketCapMin || universe.marketCapMax) && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Market Cap Range:</span>
                    <span className="font-medium text-gray-900">
                      {formatMarketCap(universe.marketCapMin)} - {formatMarketCap(universe.marketCapMax)}
                    </span>
                  </div>
                )}

                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Last Updated:</span>
                  <span className="font-medium text-gray-900">{formatDate(universe.lastUpdated)}</span>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
