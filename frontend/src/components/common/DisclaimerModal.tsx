import { useState } from 'react';
import { disclaimerService } from '../../services/disclaimerService';

interface DisclaimerModalProps {
  isOpen: boolean;
  onAccept: () => void;
  onDecline?: () => void;
}

export default function DisclaimerModal({ isOpen, onAccept, onDecline }: DisclaimerModalProps) {
  const [accepted, setAccepted] = useState(false);

  if (!isOpen) return null;

  const handleAccept = () => {
    if (!accepted) {
      alert('Please check the box to confirm you have read and understood the disclaimer.');
      return;
    }

    disclaimerService.accept();
    onAccept();
  };

  const handleDecline = () => {
    setAccepted(false);
    if (onDecline) {
      onDecline();
    }
  };

  const disclaimerText = disclaimerService.getDisclaimerText();

  return (
    <div className="fixed inset-0 bg-gray-900 bg-opacity-75 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Important Disclaimer</h2>
          <p className="text-sm text-gray-600 mt-1">Please read carefully before proceeding</p>
        </div>

        {/* Disclaimer Content - Scrollable */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          <div className="prose prose-sm max-w-none">
            <pre className="whitespace-pre-wrap text-sm text-gray-700 font-sans">{disclaimerText}</pre>
          </div>
        </div>

        {/* Acceptance Checkbox */}
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
          <label className="flex items-start cursor-pointer">
            <input
              type="checkbox"
              checked={accepted}
              onChange={(e) => setAccepted(e.target.checked)}
              className="mt-1 h-5 w-5 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <span className="ml-3 text-sm text-gray-700">
              I have read and understood this disclaimer. I acknowledge that this tool does not provide financial
              advice and that I am solely responsible for my investment decisions.
            </span>
          </label>
        </div>

        {/* Action Buttons */}
        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
          {onDecline && (
            <button
              onClick={handleDecline}
              className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded hover:bg-gray-50 transition-colors"
            >
              Decline
            </button>
          )}
          <button
            onClick={handleAccept}
            disabled={!accepted}
            className={`px-6 py-2 rounded font-semibold transition-colors ${
              accepted
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            I Accept
          </button>
        </div>
      </div>
    </div>
  );
}
