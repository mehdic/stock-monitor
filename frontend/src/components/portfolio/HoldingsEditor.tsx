import { useState } from 'react';
import { Holding } from '../../types';

interface HoldingsEditorProps {
  holdings: Holding[];
  onUpdate: (holdingId: string, data: Partial<Holding>) => Promise<void>;
  onDelete: (holdingId: string) => Promise<void>;
  validationErrors?: ValidationError[];
}

interface ValidationError {
  errorCode: string;
  rowNumber: number;
  column: string;
  message: string;
}

export default function HoldingsEditor({ holdings, onUpdate, onDelete, validationErrors }: HoldingsEditorProps) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editData, setEditData] = useState<Partial<Holding>>({});
  const [saving, setSaving] = useState(false);

  const startEdit = (holding: Holding) => {
    setEditingId(holding.id);
    setEditData({
      symbol: holding.symbol,
      quantity: holding.quantity,
      costBasisPerShare: holding.costBasisPerShare,
      currency: holding.currency,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({});
  };

  const saveEdit = async (holdingId: string) => {
    setSaving(true);
    try {
      await onUpdate(holdingId, editData);
      setEditingId(null);
      setEditData({});
    } catch (error) {
      console.error('Failed to update holding:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (holdingId: string) => {
    if (window.confirm('Are you sure you want to delete this holding?')) {
      try {
        await onDelete(holdingId);
      } catch (error) {
        console.error('Failed to delete holding:', error);
      }
    }
  };

  const getRowError = (index: number): ValidationError | undefined => {
    return validationErrors?.find((err) => err.rowNumber === index + 2); // +2 because row 1 is header
  };

  const getFieldError = (index: number, column: string): string | undefined => {
    const rowError = getRowError(index);
    return rowError?.column === column ? rowError.message : undefined;
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Symbol
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Quantity
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Cost Basis/Share
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Currency
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {holdings.map((holding, index) => {
            const isEditing = editingId === holding.id;
            const rowError = getRowError(index);
            const hasError = !!rowError;

            return (
              <tr key={holding.id} className={hasError ? 'bg-red-50' : ''}>
                <td className="px-6 py-4 whitespace-nowrap">
                  {isEditing ? (
                    <div>
                      <input
                        type="text"
                        value={editData.symbol || ''}
                        onChange={(e) => setEditData({ ...editData, symbol: e.target.value })}
                        className={`block w-full px-2 py-1 border rounded ${
                          getFieldError(index, 'symbol') ? 'border-red-500' : 'border-gray-300'
                        }`}
                      />
                      {getFieldError(index, 'symbol') && (
                        <p className="text-xs text-red-600 mt-1">{getFieldError(index, 'symbol')}</p>
                      )}
                    </div>
                  ) : (
                    <span className="text-sm font-medium text-gray-900">{holding.symbol}</span>
                  )}
                </td>

                <td className="px-6 py-4 whitespace-nowrap">
                  {isEditing ? (
                    <div>
                      <input
                        type="number"
                        step="0.01"
                        value={editData.quantity || ''}
                        onChange={(e) => setEditData({ ...editData, quantity: parseFloat(e.target.value) })}
                        className={`block w-full px-2 py-1 border rounded ${
                          getFieldError(index, 'quantity') ? 'border-red-500' : 'border-gray-300'
                        }`}
                      />
                      {getFieldError(index, 'quantity') && (
                        <p className="text-xs text-red-600 mt-1">{getFieldError(index, 'quantity')}</p>
                      )}
                    </div>
                  ) : (
                    <span className="text-sm text-gray-900">{holding.quantity}</span>
                  )}
                </td>

                <td className="px-6 py-4 whitespace-nowrap">
                  {isEditing ? (
                    <div>
                      <input
                        type="number"
                        step="0.01"
                        value={editData.costBasisPerShare || ''}
                        onChange={(e) =>
                          setEditData({ ...editData, costBasisPerShare: parseFloat(e.target.value) })
                        }
                        className={`block w-full px-2 py-1 border rounded ${
                          getFieldError(index, 'cost_basis_per_share') ? 'border-red-500' : 'border-gray-300'
                        }`}
                      />
                      {getFieldError(index, 'cost_basis_per_share') && (
                        <p className="text-xs text-red-600 mt-1">
                          {getFieldError(index, 'cost_basis_per_share')}
                        </p>
                      )}
                    </div>
                  ) : (
                    <span className="text-sm text-gray-900">${holding.costBasisPerShare.toFixed(2)}</span>
                  )}
                </td>

                <td className="px-6 py-4 whitespace-nowrap">
                  {isEditing ? (
                    <div>
                      <select
                        value={editData.currency || 'USD'}
                        onChange={(e) => setEditData({ ...editData, currency: e.target.value })}
                        className={`block w-full px-2 py-1 border rounded ${
                          getFieldError(index, 'currency') ? 'border-red-500' : 'border-gray-300'
                        }`}
                      >
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                        <option value="JPY">JPY</option>
                        <option value="CAD">CAD</option>
                        <option value="AUD">AUD</option>
                      </select>
                      {getFieldError(index, 'currency') && (
                        <p className="text-xs text-red-600 mt-1">{getFieldError(index, 'currency')}</p>
                      )}
                    </div>
                  ) : (
                    <span className="text-sm text-gray-900">{holding.currency}</span>
                  )}
                </td>

                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {isEditing ? (
                    <div className="flex space-x-2">
                      <button
                        onClick={() => saveEdit(holding.id)}
                        disabled={saving}
                        className="text-green-600 hover:text-green-900 disabled:opacity-50"
                      >
                        Save
                      </button>
                      <button
                        onClick={cancelEdit}
                        disabled={saving}
                        className="text-gray-600 hover:text-gray-900 disabled:opacity-50"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <div className="flex space-x-2">
                      <button
                        onClick={() => startEdit(holding)}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(holding.id)}
                        className="text-red-600 hover:text-red-900"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {/* Show validation errors summary */}
      {validationErrors && validationErrors.length > 0 && (
        <div className="mt-4 bg-red-50 border border-red-200 rounded p-4">
          <p className="font-medium text-red-800">Validation Errors:</p>
          <ul className="mt-2 list-disc list-inside text-sm text-red-700">
            {validationErrors.map((error, idx) => (
              <li key={idx}>{error.message}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
