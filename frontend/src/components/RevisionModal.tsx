import React, { useState } from 'react';
import { api } from '../api';
import { PlanRevisionProposal } from '../types';
import { X, CheckCircle2, AlertTriangle, ArrowRight, Sparkles } from 'lucide-react';

interface RevisionModalProps {
  isOpen: boolean;
  proposal: PlanRevisionProposal | null;
  currency: string;
  onClose: () => void;
  onSuccess: () => void;
}

export const RevisionModal: React.FC<RevisionModalProps> = ({
  isOpen,
  proposal,
  currency,
  onClose,
  onSuccess,
}) => {
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !proposal) return null;

  const handleApply = async () => {
    setApplying(true);
    setError(null);

    try {
      await api.applyRevision(proposal.eventId, proposal);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to apply plan revision.');
      }
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content wide">
        <div className="modal-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ color: 'var(--accent-sage)' }}>
                <Sparkles size={20} />
              </span>
              <h3 className="modal-title">Proposed Plan Revision Preview</h3>
            </div>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
              Review proposed changes before applying them to your event workspace.
            </p>
          </div>
          <button type="button" className="btn-subtle" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>

        <div className="modal-body">
          {error && (
            <div className="alert-banner error">
              <span>{error}</span>
            </div>
          )}

          {/* User Request & Change Summary */}
          <div
            style={{
              backgroundColor: 'var(--bg-subtle)',
              padding: '14px 16px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
              Your Revision Request
            </div>
            <p style={{ fontSize: '13px', fontStyle: 'italic', color: 'var(--text-main)', margin: '4px 0 10px' }}>
              "{proposal.prompt}"
            </p>
            <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
              Assistant Summary
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-main)', marginTop: '4px' }}>
              {proposal.changeSummary}
            </p>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '12px',
              color: 'var(--primary-forest)',
              backgroundColor: 'var(--bg-sage-light)',
              padding: '8px 12px',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <CheckCircle2 size={15} style={{ flexShrink: 0 }} />
            <span>Manual edits and confirmed line items are strictly protected and will not be overwritten.</span>
          </div>

          {/* Over-budget warning if applicable */}
          {proposal.isOverBudget && (
            <div className="alert-banner warning">
              <AlertTriangle size={18} style={{ flexShrink: 0 }} />
              <div>
                <strong>Over Target Budget:</strong> The proposed total estimated spending is{' '}
                {currency} {proposal.proposedTotalEstimated.toLocaleString()}, which exceeds your set budget limit.
              </div>
            </div>
          )}

          {/* Budget Adjustments */}
          {proposal.budgetAdjustments && proposal.budgetAdjustments.length > 0 && (
            <div>
              <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '10px', color: 'var(--primary-forest)' }}>
                Budget Adjustments ({proposal.budgetAdjustments.length})
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {proposal.budgetAdjustments.map((diff, i) => (
                  <div
                    key={i}
                    className={`diff-card diff-${diff.action.toLowerCase()}`}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span
                          className={`badge ${
                            diff.action === 'ADD'
                              ? 'badge-confirmed'
                              : diff.action === 'REMOVE'
                              ? 'badge-danger'
                              : 'badge-neutral'
                          }`}
                        >
                          {diff.action}
                        </span>
                        <strong style={{ fontSize: '13px' }}>{diff.category}</strong>
                      </div>
                      <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                        {diff.description || 'Estimated item'}
                      </span>
                      {diff.reason && (
                        <span style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '2px' }}>
                          Reason: {diff.reason}
                        </span>
                      )}
                    </div>

                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 700 }}>
                        {diff.action === 'UPDATE' ? (
                          <>
                            <span style={{ color: 'var(--text-subtle)', textDecoration: 'line-through' }}>
                              {currency} {diff.oldAmount.toLocaleString()}
                            </span>
                            <ArrowRight size={14} style={{ color: 'var(--text-subtle)' }} />
                            <span style={{ color: 'var(--primary-forest)' }}>
                              {currency} {diff.newAmount.toLocaleString()}
                            </span>
                          </>
                        ) : diff.action === 'ADD' ? (
                          <span style={{ color: 'var(--accent-sage)' }}>
                            +{currency} {diff.newAmount.toLocaleString()}
                          </span>
                        ) : (
                          <span style={{ color: 'var(--badge-danger-text)' }}>
                            -{currency} {diff.oldAmount.toLocaleString()}
                          </span>
                        )}
                      </div>
                      <span className="badge badge-estimate" style={{ marginTop: '4px' }}>
                        ESTIMATE
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Task Adjustments */}
          {proposal.taskAdjustments && proposal.taskAdjustments.length > 0 && (
            <div>
              <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '10px', color: 'var(--primary-forest)' }}>
                Checklist Updates ({proposal.taskAdjustments.length})
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {proposal.taskAdjustments.map((diff, i) => (
                  <div key={i} className="diff-card diff-add">
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span className="badge badge-confirmed">{diff.action}</span>
                        <strong style={{ fontSize: '13px' }}>{diff.title}</strong>
                        {diff.priority && (
                          <span className="badge badge-neutral">{diff.priority}</span>
                        )}
                      </div>
                      {diff.description && (
                        <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '3px' }}>
                          {diff.description}
                        </p>
                      )}
                      {diff.reason && (
                        <p style={{ fontSize: '11px', color: 'var(--text-subtle)', marginTop: '2px' }}>
                          Reason: {diff.reason}
                        </p>
                      )}
                    </div>
                    {diff.dueDate && (
                      <span style={{ fontSize: '12px', color: 'var(--text-muted)', flexShrink: 0 }}>
                        Due: {diff.dueDate}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" className="btn-secondary" onClick={onClose} disabled={applying}>
            Discard Proposal
          </button>
          <button type="button" className="btn-primary" onClick={handleApply} disabled={applying}>
            <CheckCircle2 size={16} />
            {applying ? 'Applying Revisions...' : 'Accept & Apply Revision'}
          </button>
        </div>
      </div>
    </div>
  );
};
