import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { EventItem, ImpactAnalysisResponse } from '../types';
import { X, TrendingUp, AlertTriangle, Calendar, Clock, DollarSign, Users, CheckCircle2, ArrowRight } from 'lucide-react';

interface ImpactAnalysisModalProps {
  isOpen: boolean;
  event: EventItem | null;
  onClose: () => void;
  onEventUpdated: () => void;
}

export const ImpactAnalysisModal: React.FC<ImpactAnalysisModalProps> = ({
  isOpen,
  event,
  onClose,
  onEventUpdated,
}) => {
  const [guestCount, setGuestCount] = useState<number>(event?.guestCount || 50);
  const [eventDate, setEventDate] = useState<string>(event?.eventDate || '');
  const [totalBudget, setTotalBudget] = useState<number>(event?.totalBudget || 50000);
  const [analysis, setAnalysis] = useState<ImpactAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  useEffect(() => {
    if (event) {
      setGuestCount(event.guestCount || 50);
      setEventDate(event.eventDate || '');
      setTotalBudget(event.totalBudget || 50000);
      setAnalysis(null);
      setError(null);
      setSuccessNotice(null);
    }
  }, [event, isOpen]);

  if (!isOpen || !event) return null;

  const runAnalysis = async () => {
    setLoading(true);
    setError(null);
    setSuccessNotice(null);

    try {
      const res = await api.analyzeImpact(event.id, {
        guestCount: Number(guestCount) || 1,
        eventDate: eventDate || undefined,
        totalBudget: Number(totalBudget) || 0,
      });
      setAnalysis(res);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to compute impact analysis.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleApplyChanges = async () => {
    setApplying(true);
    setError(null);

    try {
      await api.updateEvent(event.id, {
        guestCount: Number(guestCount) || 1,
        eventDate: eventDate || null,
        totalBudget: Number(totalBudget) || 0,
      });
      setSuccessNotice('Event parameters updated successfully with simulated values.');
      onEventUpdated();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to apply changes.');
      }
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content wide" style={{ maxHeight: '90vh', overflowY: 'auto' }}>
        <div className="modal-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ color: 'var(--accent-sage)' }}>
                <TrendingUp size={22} />
              </span>
              <h3 className="modal-title">Event Change Impact Analysis</h3>
            </div>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
              Simulate changes to guest headcount, celebration date, or budget before making adjustments.
            </p>
          </div>
          <button type="button" className="btn-subtle" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {error && (
            <div className="alert-banner error">
              <span>{error}</span>
            </div>
          )}

          {successNotice && (
            <div className="alert-banner success">
              <CheckCircle2 size={16} />
              <span>{successNotice}</span>
            </div>
          )}

          {/* Simulation Controls */}
          <div
            style={{
              backgroundColor: 'var(--bg-subtle)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase', marginBottom: '12px' }}>
              Simulate New Parameters
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="sim-guests">
                  <Users size={14} style={{ display: 'inline', marginRight: '4px' }} />
                  Guest Count (Current: {event.guestCount || '—'})
                </label>
                <input
                  id="sim-guests"
                  type="number"
                  min={1}
                  className="form-input"
                  value={guestCount}
                  onChange={(e) => setGuestCount(Math.max(1, parseInt(e.target.value) || 1))}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="sim-date">
                  <Calendar size={14} style={{ display: 'inline', marginRight: '4px' }} />
                  Celebration Date (Current: {event.eventDate || '—'})
                </label>
                <input
                  id="sim-date"
                  type="date"
                  className="form-input"
                  value={eventDate}
                  onChange={(e) => setEventDate(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="sim-budget">
                  <DollarSign size={14} style={{ display: 'inline', marginRight: '4px' }} />
                  Target Budget ({event.currency})
                </label>
                <input
                  id="sim-budget"
                  type="number"
                  min={0}
                  step={1000}
                  className="form-input"
                  value={totalBudget}
                  onChange={(e) => setTotalBudget(Math.max(0, parseFloat(e.target.value) || 0))}
                />
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '14px' }}>
              <button
                type="button"
                className="btn-primary"
                onClick={runAnalysis}
                disabled={loading}
              >
                <TrendingUp size={16} />
                {loading ? 'Analyzing Consequences...' : 'Analyze Impact'}
              </button>
            </div>
          </div>

          {/* Analysis Results */}
          {analysis && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Warnings and Key Metrics Banner */}
              {analysis.budgetImpact.isProjectedOverBudget && (
                <div className="alert-banner warning">
                  <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                  <div>
                    <strong>Projected Over-Budget Condition:</strong> Current committed and estimated expenses exceed the proposed budget by{' '}
                    {analysis.currency} {(analysis.budgetImpact.currentEstimatedTotal - analysis.budgetImpact.proposedBudget).toLocaleString()}.
                  </div>
                </div>
              )}

              {analysis.timelineImpact.isScheduleCompressed && (
                <div className="alert-banner warning">
                  <Clock size={18} style={{ flexShrink: 0 }} />
                  <div>
                    <strong>Schedule Compressed by {Math.abs(analysis.timelineImpact.daysShifted)} Days:</strong> Celebration date moved earlier. {analysis.timelineImpact.affectedTasksCount} task(s) require rescheduling.
                  </div>
                </div>
              )}

              {/* KPI Comparison Cards */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: '12px' }}>
                <div className="section-card" style={{ padding: '14px' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-subtle)', fontWeight: 700 }}>BUDGET DELTA</span>
                  <div style={{ fontSize: '18px', fontWeight: 800, marginTop: '4px', color: analysis.budgetImpact.budgetDelta >= 0 ? 'var(--primary-forest)' : 'var(--badge-danger-text)' }}>
                    {analysis.budgetImpact.budgetDelta >= 0 ? '+' : ''}{analysis.currency} {analysis.budgetImpact.budgetDelta.toLocaleString()}
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    New: {analysis.currency} {analysis.budgetImpact.proposedBudget.toLocaleString()}
                  </span>
                </div>

                <div className="section-card" style={{ padding: '14px' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-subtle)', fontWeight: 700 }}>PER-GUEST BUDGET</span>
                  <div style={{ fontSize: '18px', fontWeight: 800, marginTop: '4px' }}>
                    {analysis.currency} {analysis.budgetImpact.perGuestProposed.toLocaleString()}
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    Was: {analysis.currency} {analysis.budgetImpact.perGuestOriginal.toLocaleString()}
                  </span>
                </div>

                <div className="section-card" style={{ padding: '14px' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-subtle)', fontWeight: 700 }}>DAYS UNTIL EVENT</span>
                  <div style={{ fontSize: '18px', fontWeight: 800, marginTop: '4px' }}>
                    {analysis.timelineImpact.proposedDaysRemaining !== null ? `${analysis.timelineImpact.proposedDaysRemaining} days` : '—'}
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    Shift: {analysis.timelineImpact.daysShifted >= 0 ? `+${analysis.timelineImpact.daysShifted}` : analysis.timelineImpact.daysShifted} days
                  </span>
                </div>

                <div className="section-card" style={{ padding: '14px' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-subtle)', fontWeight: 700 }}>PROJECTED BALANCE</span>
                  <div style={{ fontSize: '18px', fontWeight: 800, marginTop: '4px', color: analysis.budgetImpact.projectedRemaining >= 0 ? 'var(--primary-forest)' : 'var(--badge-danger-text)' }}>
                    {analysis.currency} {analysis.budgetImpact.projectedRemaining.toLocaleString()}
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    {analysis.budgetImpact.projectedRemaining >= 0 ? 'Remaining headroom' : 'Deficit'}
                  </span>
                </div>
              </div>

              {/* Headcount Sensitive Categories */}
              {analysis.budgetImpact.categoryImpacts && analysis.budgetImpact.categoryImpacts.length > 0 && (
                <div className="section-card">
                  <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '8px', color: 'var(--primary-forest)' }}>
                    Headcount-Sensitive Category Impacts
                  </h4>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Category</th>
                        <th style={{ textAlign: 'right' }}>Current Allocation</th>
                        <th style={{ textAlign: 'right' }}>Projected Requirement</th>
                        <th style={{ textAlign: 'right' }}>Variance</th>
                        <th>Explanation</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analysis.budgetImpact.categoryImpacts.map((cat, i) => (
                        <tr key={i}>
                          <td><strong>{cat.category}</strong></td>
                          <td style={{ textAlign: 'right' }}>
                            {analysis.currency} {cat.originalEstimate.toLocaleString()}
                          </td>
                          <td style={{ textAlign: 'right', fontWeight: 600 }}>
                            {analysis.currency} {cat.projectedEstimate.toLocaleString()}
                          </td>
                          <td style={{ textAlign: 'right', color: cat.delta > 0 ? 'var(--badge-danger-text)' : 'var(--primary-forest)', fontWeight: 700 }}>
                            {cat.delta > 0 ? '+' : ''}{analysis.currency} {cat.delta.toLocaleString()}
                          </td>
                          <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{cat.explanation}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {/* Impacted Tasks */}
              {analysis.timelineImpact.taskWarnings && analysis.timelineImpact.taskWarnings.length > 0 && (
                <div className="section-card">
                  <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '8px', color: 'var(--badge-danger-text)' }}>
                    Impacted Task Deadlines ({analysis.timelineImpact.taskWarnings.length})
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {analysis.timelineImpact.taskWarnings.map((w, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '10px 14px',
                          borderRadius: 'var(--radius-sm)',
                          backgroundColor: 'var(--bg-subtle)',
                          border: '1px solid var(--border-subtle)',
                        }}
                      >
                        <div>
                          <strong>{w.taskTitle}</strong>
                          <p style={{ fontSize: '12px', color: 'var(--badge-danger-text)', marginTop: '2px' }}>
                            {w.warningReason}
                          </p>
                        </div>
                        {w.suggestedNewDueDate && (
                          <div style={{ textAlign: 'right', fontSize: '12px' }}>
                            <span style={{ color: 'var(--text-subtle)' }}>Suggested New Due Date:</span>
                            <div style={{ fontWeight: 700, color: 'var(--primary-forest)' }}>
                              {w.suggestedNewDueDate}
                            </div>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Actionable Recommendations */}
              <div className="section-card">
                <h4 style={{ fontSize: '13px', fontWeight: 700, marginBottom: '8px', color: 'var(--primary-forest)' }}>
                  Actionable Recommendations & Next Steps
                </h4>
                <ul style={{ paddingLeft: '18px', fontSize: '13px', color: 'var(--text-main)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {analysis.recommendations.map((rec, rIdx) => (
                    <li key={rIdx}>{rec}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
          {analysis && (
            <button
              type="button"
              className="btn-primary"
              onClick={handleApplyChanges}
              disabled={applying}
            >
              <ArrowRight size={16} />
              {applying ? 'Applying Parameters...' : 'Apply Simulated Parameters to Event'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
