import { useState } from 'react';
import ExclusionsList from '../components/recommendations/ExclusionsList';
import ExclusionDetailModal from '../components/recommendations/ExclusionDetailModal';
import NoTradeBanner from '../components/recommendations/NoTradeBanner';

/**
 * Recommendations page with exclusions tab (T199, T204).
 */
export default function Recommendations() {
  const [activeTab, setActiveTab] = useState<'recommendations' | 'exclusions'>('recommendations');
  const [selectedExclusion, setSelectedExclusion] = useState<any>(null);
  const runId = 'latest-run-id'; // TODO: Get from route/context
  const shouldShowNoTrade = false; // TODO: Get from run status

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Recommendations</h1>
        <p className="text-gray-600 mt-2">
          Review month-end recommendations and understand exclusions
        </p>
      </div>

      {/* No Trade Banner */}
      {shouldShowNoTrade && <NoTradeBanner />}

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('recommendations')}
            className={`${
              activeTab === 'recommendations'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
          >
            Recommendations
          </button>
          <button
            onClick={() => setActiveTab('exclusions')}
            className={`${
              activeTab === 'exclusions'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
          >
            Exclusions
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'recommendations' && (
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-600">Recommendations list will be displayed here</p>
        </div>
      )}

      {activeTab === 'exclusions' && (
        <ExclusionsList runId={runId} onShowDetail={setSelectedExclusion} />
      )}

      {/* Exclusion Detail Modal */}
      <ExclusionDetailModal
        exclusion={selectedExclusion}
        onClose={() => setSelectedExclusion(null)}
      />
    </div>
  );
}
