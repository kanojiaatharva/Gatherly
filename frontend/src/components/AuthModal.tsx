import React, { useState } from 'react';
import { api } from '../api';
import { User } from '../types';
import { X, Lock, Mail, User as UserIcon } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (user: User) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (mode === 'register') {
        const res = await api.register(email, password, displayName);
        onSuccess(res.user);
      } else {
        const res = await api.login(email, password);
        onSuccess(res.user);
      }
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Authentication failed. Please check your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-content" style={{ maxWidth: '440px' }}>
        <div className="modal-header">
          <h3 className="modal-title">
            {mode === 'login' ? 'Welcome back to Gatherly' : 'Create your planner workspace'}
          </h3>
          <button type="button" className="btn-subtle" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div
              style={{
                display: 'flex',
                borderBottom: '1px solid var(--border-subtle)',
                marginBottom: '10px',
              }}
            >
              <button
                type="button"
                className={`btn-subtle ${mode === 'login' ? 'active' : ''}`}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderBottom: mode === 'login' ? '2px solid var(--primary-forest)' : 'none',
                  borderRadius: 0,
                  fontWeight: mode === 'login' ? 700 : 500,
                  color: mode === 'login' ? 'var(--primary-forest)' : 'var(--text-muted)',
                }}
                onClick={() => {
                  setMode('login');
                  setError(null);
                }}
              >
                Sign In
              </button>
              <button
                type="button"
                className={`btn-subtle ${mode === 'register' ? 'active' : ''}`}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderBottom: mode === 'register' ? '2px solid var(--primary-forest)' : 'none',
                  borderRadius: 0,
                  fontWeight: mode === 'register' ? 700 : 500,
                  color: mode === 'register' ? 'var(--primary-forest)' : 'var(--text-muted)',
                }}
                onClick={() => {
                  setMode('register');
                  setError(null);
                }}
              >
                Create Account
              </button>
            </div>

            {error && (
              <div className="alert-banner error">
                <span>{error}</span>
              </div>
            )}

            {mode === 'register' && (
              <div className="form-group">
                <label className="form-label" htmlFor="auth-name">
                  Full Name
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="auth-name"
                    type="text"
                    className="form-input"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="e.g. Sarah Miller"
                    required
                  />
                  <span
                    style={{
                      position: 'absolute',
                      right: '10px',
                      top: '9px',
                      color: 'var(--text-subtle)',
                    }}
                  >
                    <UserIcon size={16} />
                  </span>
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="auth-email">
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  id="auth-email"
                  type="email"
                  className="form-input"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  required
                />
                <span
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '9px',
                    color: 'var(--text-subtle)',
                  }}
                >
                  <Mail size={16} />
                </span>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="auth-password">
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  id="auth-password"
                  type="password"
                  className="form-input"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  minLength={6}
                  required
                />
                <span
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '9px',
                    color: 'var(--text-subtle)',
                  }}
                >
                  <Lock size={16} />
                </span>
              </div>
              {mode === 'register' && (
                <span className="form-hint">Must be at least 6 characters long</span>
              )}
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading
                ? 'Processing...'
                : mode === 'login'
                ? 'Sign In to Workspace'
                : 'Create Account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
