import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { constraintService, ConstraintSetDTO, ConstraintPreviewDTO } from '../services/constraintService';
import ConstraintInput from '../components/settings/ConstraintInput';
import ConstraintPreview from '../components/settings/ConstraintPreview';

/**
 * Settings page with constraint tuning (T142, T146).
 *
 * Features:
 * - Constraint editor with sliders and tooltips (FR-016)
 * - Real-time impact preview (FR-017)
 * - Save changes with validation (FR-019)
 * - Reset to defaults with confirmation (FR-018)
 */
export default function SettingsPage() {
  const queryClient = useQueryClient();
  const portfolioId = 'default-portfolio'; // TODO: Get from context/route

  // State
  const [constraints, setConstraints] = useState<ConstraintSetDTO | null>(null);
  const [preview, setPreview] = useState<ConstraintPreviewDTO | null>(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Fetch current constraints
  const { data: currentConstraints, isLoading } = useQuery({
    queryKey: ['constraints', portfolioId],
    queryFn: () => constraintService.getConstraintsForPortfolio(portfolioId),
  });

  // Initialize local state when data loads
  useEffect(() => {
    if (currentConstraints && !constraints) {
      setConstraints(currentConstraints);
    }
  }, [currentConstraints, constraints]);

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: (data: ConstraintSetDTO) =>
      constraintService.saveConstraints(portfolioId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['constraints', portfolioId] });
      setPreview(null);
      alert('Constraints saved successfully!');
    },
    onError: (error: any) => {
      const errors = error.response?.data?.fieldErrors || {};
      setValidationErrors(errors);
      alert('Failed to save constraints. Please check validation errors.');
    },
  });

  // Reset mutation
  const resetMutation = useMutation({
    mutationFn: () => constraintService.resetToDefaults(portfolioId),
    onSuccess: (data) => {
      setConstraints(data);
      setPreview(null);
      setValidationErrors({});
      queryClient.invalidateQueries({ queryKey: ['constraints', portfolioId] });
      setShowResetModal(false);
      alert('Constraints reset to defaults successfully!');
    },
    onError: () => {
      alert('Failed to reset constraints.');
    },
  });

  // Handle constraint change
  const handleConstraintChange = (name: string, value: number) => {
    if (!constraints) return;

    setConstraints({
      ...constraints,
      [name]: value,
    });

    // Clear preview when constraints change
    setPreview(null);
    // Clear validation error for this field
    setValidationErrors((prev) => {
      const updated = { ...prev };
      delete updated[name];
      return updated;
    });
  };

  // Preview impact
  const handlePreview = async () => {
    if (!constraints) return;

    setIsPreviewLoading(true);
    try {
      const previewResult = await constraintService.previewImpact(portfolioId, constraints);
      setPreview(previewResult);
    } catch (error: any) {
      alert('Failed to generate preview: ' + (error.response?.data?.message || error.message));
    } finally {
      setIsPreviewLoading(false);
    }
  };

  // Save constraints
  const handleSave = () => {
    if (!constraints) return;
    saveMutation.mutate(constraints);
  };

  // Reset to defaults
  const handleResetConfirm = () => {
    resetMutation.mutate();
  };

  if (isLoading || !constraints) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin h-8 w-8 border-b-2 border-blue-600 rounded-full"></div>
        <span className="ml-3 text-gray-600">Loading constraints...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Portfolio Constraints</h1>
          <p className="text-gray-600 mt-2">
            Adjust risk parameters and preview impact before saving
          </p>
        </div>
        <button
          onClick={() => setShowResetModal(true)}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          Restore Defaults
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Constraint Editor */}
        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          <h2 className="text-xl font-semibold text-gray-900">Edit Constraints</h2>

          <ConstraintInput
            label="Max Position Size"
            name="maxPositionSizePct"
            value={constraints.maxPositionSizePct}
            onChange={handleConstraintChange}
            min={0}
            max={100}
            step={0.5}
            unit="%"
            tooltip="Maximum percentage of portfolio allocated to a single position"
            error={validationErrors.maxPositionSizePct}
          />

          <ConstraintInput
            label="Max Sector Exposure"
            name="maxSectorExposurePct"
            value={constraints.maxSectorExposurePct}
            onChange={handleConstraintChange}
            min={0}
            max={100}
            step={1}
            unit="%"
            tooltip="Maximum percentage of portfolio allocated to any single sector"
            error={validationErrors.maxSectorExposurePct}
          />

          <ConstraintInput
            label="Max Turnover"
            name="maxTurnoverPct"
            value={constraints.maxTurnoverPct}
            onChange={handleConstraintChange}
            min={0}
            max={100}
            step={1}
            unit="%"
            tooltip="Maximum portfolio turnover allowed per rebalance"
            error={validationErrors.maxTurnoverPct}
          />

          <ConstraintInput
            label="Min Market Cap"
            name="minMarketCapBn"
            value={constraints.minMarketCapBn}
            onChange={handleConstraintChange}
            min={0}
            max={500}
            step={1}
            unit="B"
            tooltip="Minimum market capitalization in billions for eligible stocks"
            error={validationErrors.minMarketCapBn}
          />

          <ConstraintInput
            label="Cash Buffer"
            name="cashBufferPct"
            value={constraints.cashBufferPct}
            onChange={handleConstraintChange}
            min={0}
            max={50}
            step={0.5}
            unit="%"
            tooltip="Percentage of portfolio held in cash as buffer"
            error={validationErrors.cashBufferPct}
          />

          <ConstraintInput
            label="Min Liquidity Tier"
            name="minLiquidityTier"
            value={constraints.minLiquidityTier}
            onChange={handleConstraintChange}
            min={1}
            max={5}
            step={1}
            unit=""
            tooltip="Minimum liquidity tier (1=most liquid, 5=least liquid)"
            error={validationErrors.minLiquidityTier}
          />

          {/* Action Buttons */}
          <div className="pt-4 space-y-3">
            <button
              onClick={handlePreview}
              disabled={isPreviewLoading}
              className="w-full px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:bg-blue-300"
            >
              {isPreviewLoading ? 'Calculating...' : 'Preview Impact'}
            </button>

            <button
              onClick={handleSave}
              disabled={saveMutation.isPending || !preview}
              className="w-full px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-lg hover:bg-green-700 disabled:bg-gray-300"
            >
              {saveMutation.isPending ? 'Saving...' : 'Save Changes'}
            </button>

            {!preview && (
              <p className="text-xs text-gray-500 text-center">
                Preview impact before saving changes
              </p>
            )}
          </div>
        </div>

        {/* Right Column: Preview Panel */}
        <div>
          <ConstraintPreview preview={preview} isLoading={isPreviewLoading} />
        </div>
      </div>

      {/* Reset Confirmation Modal */}
      {showResetModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Reset to Default Constraints?
            </h3>
            <p className="text-gray-600 mb-6">
              This will discard all current customizations and restore the default constraint
              values. This action cannot be undone.
            </p>
            <div className="flex space-x-3">
              <button
                onClick={() => setShowResetModal(false)}
                className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleResetConfirm}
                disabled={resetMutation.isPending}
                className="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:bg-red-300"
              >
                {resetMutation.isPending ? 'Resetting...' : 'Reset to Defaults'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
