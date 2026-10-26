import React, { useState } from 'react';
import { api } from '../api';
import { VendorItem } from '../types';
import { X, Plus, Save, Info, CheckCircle2 } from 'lucide-react';

interface AddVendorModalProps {
  isOpen: boolean;
  eventId: number;
  vendorToEdit?: VendorItem | null;
  currency: string;
  onClose: () => void;
  onSuccess: () => void;
}

export const AddVendorModal: React.FC<AddVendorModalProps> = ({
  isOpen,
  eventId,
  vendorToEdit,
  currency,
  onClose,
  onSuccess,
}) => {
  const [category, setCategory] = useState(vendorToEdit?.category || 'Photography');
  const [name, setName] = useState(vendorToEdit?.name || '');
  const [contactDetails, setContactDetails] = useState(vendorToEdit?.contactDetails || '');
  const [quotedAmount, setQuotedAmount] = useState<number | ''>(
    vendorToEdit?.quotedAmount !== null && vendorToEdit?.quotedAmount !== undefined
      ? vendorToEdit.quotedAmount
      : ''
  );
  const [quoteStatus, setQuoteStatus] = useState<VendorItem['quoteStatus']>(
    vendorToEdit?.quoteStatus || 'UNVERIFIED'
  );
  const [bookingStatus, setBookingStatus] = useState<VendorItem['bookingStatus']>(
    vendorToEdit?.bookingStatus || 'NOT_BOOKED'
  );
  const [source, setSource] = useState(vendorToEdit?.source || 'Direct Inquiry');
  const [notes, setNotes] = useState(vendorToEdit?.notes || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (vendorToEdit) {
        await api.updateVendor(eventId, vendorToEdit.id, {
          category,
          name: name || null,
          contactDetails: contactDetails || null,
          quotedAmount: quotedAmount !== '' ? Number(quotedAmount) : null,
          quoteStatus,
          bookingStatus,
          source: source || 'MANUAL_ENTRY',
          notes: notes || null,
        });
      } else {
        await api.createVendor(eventId, {
          category,
          name: name || null,
          contactDetails: contactDetails || null,
          quotedAmount: quotedAmount !== '' ? Number(quotedAmount) : null,
          quoteStatus,
          bookingStatus,
          source: source || 'MANUAL_ENTRY',
          notes: notes || null,
        });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to save vendor record.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content" style={{ maxWidth: '540px' }}>
        <div className="modal-header">
          <div>
            <h3 className="modal-title">{vendorToEdit ? 'Edit Vendor Record' : 'Add Tracked Vendor'}</h3>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              Organize verified quotes, vendor contacts, and booking contracts.
            </p>
          </div>
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

            <div className="alert-banner info" style={{ padding: '10px 14px' }}>
              <Info size={16} style={{ flexShrink: 0 }} />
              <span style={{ fontSize: '11px' }}>
                Gatherly only tracks real vendors you add and quote. We never invent prices, fake reviews, or pretend bookings.
              </span>
            </div>

            {vendorToEdit?.lastVerifiedAt && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: 'var(--badge-confirmed-text)', backgroundColor: 'var(--badge-confirmed-bg)', padding: '6px 12px', borderRadius: 'var(--radius-sm)' }}>
                <CheckCircle2 size={14} />
                <span>Quote verified on {new Date(vendorToEdit.lastVerifiedAt).toLocaleDateString()}</span>
              </div>
            )}

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="vendor-cat">
                  Category *
                </label>
                <input
                  id="vendor-cat"
                  type="text"
                  className="form-input"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  placeholder="e.g. Venue, Florals, Catering"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="vendor-name">
                  Vendor / Business Name
                </label>
                <input
                  id="vendor-name"
                  type="text"
                  className="form-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Silver Screen Studios"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="vendor-contact">
                  Contact Information
                </label>
                <input
                  id="vendor-contact"
                  type="text"
                  className="form-input"
                  value={contactDetails}
                  onChange={(e) => setContactDetails(e.target.value)}
                  placeholder="Phone, email, or website"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="vendor-quote">
                  Quoted Amount ({currency})
                </label>
                <input
                  id="vendor-quote"
                  type="number"
                  min={0}
                  step={100}
                  className="form-input"
                  value={quotedAmount}
                  onChange={(e) =>
                    setQuotedAmount(e.target.value === '' ? '' : Math.max(0, parseFloat(e.target.value) || 0))
                  }
                  placeholder="Official quote"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="vendor-quote-status">
                  Quote Status
                </label>
                <select
                  id="vendor-quote-status"
                  className="form-select"
                  value={quoteStatus}
                  onChange={(e) => setQuoteStatus(e.target.value as VendorItem['quoteStatus'])}
                >
                  <option value="UNVERIFIED">Unverified / Awaiting Quote</option>
                  <option value="ESTIMATED">Estimated / Rough Ballpark</option>
                  <option value="QUOTED">Official Quote Received</option>
                  <option value="REJECTED">Declined / Rejected</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="vendor-book-status">
                  Booking Status
                </label>
                <select
                  id="vendor-book-status"
                  className="form-select"
                  value={bookingStatus}
                  onChange={(e) => setBookingStatus(e.target.value as VendorItem['bookingStatus'])}
                >
                  <option value="NOT_BOOKED">Not Booked</option>
                  <option value="CONTACTED">Inquired / Contacted</option>
                  <option value="SHORTLISTED">Shortlisted</option>
                  <option value="BOOKED">Contract Signed / Booked</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="vendor-source">
                Source / Channel
              </label>
              <input
                id="vendor-source"
                type="text"
                className="form-input"
                value={source}
                onChange={(e) => setSource(e.target.value)}
                placeholder="e.g. Direct Inquiry, Personal Referral, Venue Preferred List"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="vendor-notes">
                Notes & Terms
              </label>
              <textarea
                id="vendor-notes"
                className="form-textarea"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Cancellation policies, payment schedule, deposit details"
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {vendorToEdit ? <Save size={16} /> : <Plus size={16} />}
              {loading ? 'Saving...' : vendorToEdit ? 'Save Changes' : 'Track Vendor'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
