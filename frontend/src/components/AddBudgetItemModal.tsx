import React, { useState } from 'react';
import { api } from '../api';
import { BudgetItem } from '../types';
import { X, Plus, Save } from 'lucide-react';

interface AddBudgetItemModalProps {
  isOpen: boolean;
  eventId: number;
  itemToEdit?: BudgetItem | null;
  currency: string;
  onClose: () => void;
  onSuccess: () => void;
}

export const AddBudgetItemModal: React.FC<AddBudgetItemModalProps> = ({
  isOpen,
  eventId,
  itemToEdit,
  currency,
  onClose,
  onSuccess,
}) => {
  const [category, setCategory] = useState(itemToEdit?.category || 'Catering');
  const [description, setDescription] = useState(itemToEdit?.description || '');
  const [estimatedAmount, setEstimatedAmount] = useState<number>(itemToEdit?.estimatedAmount || 0);
  const [actualAmount, setActualAmount] = useState<number | ''>(
    itemToEdit?.actualAmount !== null && itemToEdit?.actualAmount !== undefined
      ? itemToEdit.actualAmount
      : ''
  );
  const [isEstimate, setIsEstimate] = useState<boolean>(
    itemToEdit ? itemToEdit.isEstimate : true
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (itemToEdit) {
        await api.updateBudgetItem(eventId, itemToEdit.id, {
          category,
          description: description || null,
          estimatedAmount: Number(estimatedAmount) || 0,
          actualAmount: actualAmount !== '' ? Number(actualAmount) : null,
          isEstimate,
        });
      } else {
        await api.addBudgetItem(eventId, {
          category,
          description: description || null,
          estimatedAmount: Number(estimatedAmount) || 0,
          actualAmount: actualAmount !== '' ? Number(actualAmount) : null,
          isEstimate,
        });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to save budget item.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content" style={{ maxWidth: '480px' }}>
        <div className="modal-header">
          <h3 className="modal-title">
            {itemToEdit ? 'Edit Budget Item' : 'Add New Budget Item'}
          </h3>
          <button type="button" className="btn-subtle" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && (
              <div className="alert-banner error">
                <span>{error}</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="budget-category">
                Category *
              </label>
              <input
                id="budget-category"
                type="text"
                className="form-input"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. Venue, Catering, Photography, Florals"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="budget-desc">
                Description
              </label>
              <input
                id="budget-desc"
                type="text"
                className="form-input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. 3-course dinner with vegetarian options"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="budget-est">
                  Estimated Amount ({currency}) *
                </label>
                <input
                  id="budget-est"
                  type="number"
                  min={0}
                  step={100}
                  className="form-input"
                  value={estimatedAmount}
                  onChange={(e) => setEstimatedAmount(Math.max(0, parseFloat(e.target.value) || 0))}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="budget-act">
                  Actual Amount ({currency})
                </label>
                <input
                  id="budget-act"
                  type="number"
                  min={0}
                  step={100}
                  className="form-input"
                  value={actualAmount}
                  onChange={(e) => {
                    const val = e.target.value === '' ? '' : Math.max(0, parseFloat(e.target.value) || 0);
                    setActualAmount(val);
                    if (val !== '' && Number(val) > 0 && !itemToEdit) {
                      setIsEstimate(false);
                    }
                  }}
                  placeholder="Optional"
                />
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '6px' }}>
              <input
                id="budget-is-est"
                type="checkbox"
                checked={isEstimate}
                onChange={(e) => setIsEstimate(e.target.checked)}
                style={{ width: '16px', height: '16px', cursor: 'pointer' }}
              />
              <label htmlFor="budget-is-est" style={{ fontSize: '13px', cursor: 'pointer' }}>
                Mark as estimated cost (uncheck if price is backed by a verified quote)
              </label>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {itemToEdit ? <Save size={16} /> : <Plus size={16} />}
              {loading ? 'Saving...' : itemToEdit ? 'Save Changes' : 'Add Item'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
