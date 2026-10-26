import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { EventItem, EventType } from '../types';
import { X, Sparkles, Calendar, MapPin, Users, Coins, Save } from 'lucide-react';

interface EventModalProps {
  isOpen: boolean;
  eventToEdit?: EventItem | null;
  onClose: () => void;
  onSuccess: (event: EventItem) => void;
}

export const EventModal: React.FC<EventModalProps> = ({
  isOpen,
  eventToEdit,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [eventType, setEventType] = useState<EventType>('Birthday');
  const [eventDate, setEventDate] = useState('');
  const [location, setLocation] = useState('');
  const [guestCount, setGuestCount] = useState<number>(50);
  const [currency, setCurrency] = useState('INR');
  const [totalBudget, setTotalBudget] = useState<number>(50000);
  const [stylePreferences, setStylePreferences] = useState('');
  const [dietaryRequirements, setDietaryRequirements] = useState('');
  const [accessibilityNeeds, setAccessibilityNeeds] = useState('');
  const [priorities, setPriorities] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (eventToEdit) {
      setName(eventToEdit.name || '');
      setEventType((eventToEdit.eventType as EventType) || 'Birthday');
      setEventDate(eventToEdit.eventDate || '');
      setLocation(eventToEdit.location || '');
      setGuestCount(eventToEdit.guestCount || 50);
      setCurrency(eventToEdit.currency || 'INR');
      setTotalBudget(eventToEdit.totalBudget || 0);
      setStylePreferences(eventToEdit.stylePreferences || '');
      setDietaryRequirements(eventToEdit.dietaryRequirements || '');
      setAccessibilityNeeds(eventToEdit.accessibilityNeeds || '');
      setPriorities(eventToEdit.priorities || '');
    } else {
      setName('');
      setEventType('Birthday');
      setEventDate('');
      setLocation('');
      setGuestCount(50);
      setCurrency('INR');
      setTotalBudget(50000);
      setStylePreferences('');
      setDietaryRequirements('');
      setAccessibilityNeeds('');
      setPriorities('');
    }
  }, [eventToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (eventToEdit) {
        const updated = await api.updateEvent(eventToEdit.id, {
          name,
          eventType,
          eventDate: eventDate || null,
          location: location || null,
          guestCount: Number(guestCount) || 1,
          currency,
          totalBudget: Number(totalBudget) || 0,
          stylePreferences: stylePreferences || null,
          dietaryRequirements: dietaryRequirements || null,
          accessibilityNeeds: accessibilityNeeds || null,
          priorities: priorities || null,
        });
        onSuccess(updated);
      } else {
        const created = await api.createEvent({
          name,
          eventType,
          eventDate: eventDate || null,
          location: location || null,
          guestCount: Number(guestCount) || 1,
          currency,
          totalBudget: Number(totalBudget) || 0,
          stylePreferences: stylePreferences || null,
          dietaryRequirements: dietaryRequirements || null,
          accessibilityNeeds: accessibilityNeeds || null,
          priorities: priorities || null,
        });
        onSuccess(created);
      }
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to save celebration details. Please check inputs.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content wide">
        <div className="modal-header">
          <div>
            <h3 className="modal-title">
              {eventToEdit ? `Edit "${eventToEdit.name}"` : 'Create a New Celebration'}
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {eventToEdit
                ? 'Update constraints, budget limits, or celebration preferences.'
                : 'Enter key constraints to generate a personalized planning workspace.'}
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

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="event-name">
                  Event Name *
                </label>
                <input
                  id="event-name"
                  type="text"
                  className="form-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Maya's 30th Birthday"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="event-type">
                  Event Type *
                </label>
                <select
                  id="event-type"
                  className="form-select"
                  value={eventType}
                  onChange={(e) => setEventType(e.target.value as EventType)}
                  required
                >
                  <option value="Birthday">Birthday</option>
                  <option value="Wedding">Wedding</option>
                  <option value="Community event">Community event</option>
                  <option value="Anniversary">Anniversary</option>
                  <option value="Gala">Gala</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="event-date">
                  Event Date
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="event-date"
                    type="date"
                    className="form-input"
                    value={eventDate}
                    onChange={(e) => setEventDate(e.target.value)}
                  />
                  <span style={{ position: 'absolute', right: '10px', top: '9px', color: 'var(--text-subtle)' }}>
                    <Calendar size={16} />
                  </span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="event-location">
                  City / Location Preference
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="event-location"
                    type="text"
                    className="form-input"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="e.g. Olive Bistro, Mumbai"
                  />
                  <span style={{ position: 'absolute', right: '10px', top: '9px', color: 'var(--text-subtle)' }}>
                    <MapPin size={16} />
                  </span>
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="event-guests">
                  Expected Guest Count *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="event-guests"
                    type="number"
                    min={1}
                    className="form-input"
                    value={guestCount}
                    onChange={(e) => setGuestCount(Math.max(1, parseInt(e.target.value) || 1))}
                    required
                  />
                  <span style={{ position: 'absolute', right: '10px', top: '9px', color: 'var(--text-subtle)' }}>
                    <Users size={16} />
                  </span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="event-currency">
                  Currency & Target Budget *
                </label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <select
                    id="event-currency"
                    className="form-select"
                    style={{ width: '90px' }}
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                  >
                    <option value="INR">INR (₹)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                    <option value="CAD">CAD ($)</option>
                  </select>
                  <div style={{ position: 'relative', flex: 1 }}>
                    <input
                      id="event-budget"
                      type="number"
                      min={0}
                      step={500}
                      className="form-input"
                      value={totalBudget}
                      onChange={(e) => setTotalBudget(Math.max(0, parseFloat(e.target.value) || 0))}
                      required
                    />
                    <span style={{ position: 'absolute', right: '10px', top: '9px', color: 'var(--text-subtle)' }}>
                      <Coins size={16} />
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="event-style">
                Style, Atmosphere & Color Palette
              </label>
              <input
                id="event-style"
                type="text"
                className="form-input"
                value={stylePreferences}
                onChange={(e) => setStylePreferences(e.target.value)}
                placeholder="e.g. Warm botanical minimalism, fairy lights, acoustic ambient sound"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="event-diet">
                  Dietary Requirements
                </label>
                <input
                  id="event-diet"
                  type="text"
                  className="form-input"
                  value={dietaryRequirements}
                  onChange={(e) => setDietaryRequirements(e.target.value)}
                  placeholder="e.g. Vegetarian highlights, vegan dessert, nut-free"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="event-access">
                  Accessibility Needs
                </label>
                <input
                  id="event-access"
                  type="text"
                  className="form-input"
                  value={accessibilityNeeds}
                  onChange={(e) => setAccessibilityNeeds(e.target.value)}
                  placeholder="e.g. Wheelchair ramp, ground floor seating, quiet zone"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="event-priorities">
                Top Priorities
              </label>
              <textarea
                id="event-priorities"
                className="form-textarea"
                value={priorities}
                onChange={(e) => setPriorities(e.target.value)}
                placeholder="What matters most? e.g. Outstanding food & drinks, staying within budget, memorable photography."
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {eventToEdit ? <Save size={16} /> : <Sparkles size={16} />}
              {loading
                ? 'Saving...'
                : eventToEdit
                ? 'Save Event Changes'
                : 'Create Event & Launch Planner'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export const NewEventModal = EventModal;
