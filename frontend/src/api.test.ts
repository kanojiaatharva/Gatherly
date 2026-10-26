import { describe, it, expect, beforeEach, vi } from 'vitest';
import { api } from './api';

const storageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(globalThis, 'localStorage', {
  value: storageMock,
  writable: true,
});

describe('ApiClient', () => {
  beforeEach(() => {
    localStorage.clear();
    api.logout();
    vi.restoreAllMocks();
  });

  describe('Token Management', () => {
    it('stores token in localStorage when setToken is called', () => {
      api.setToken('test-jwt-token-123');
      expect(api.getToken()).toBe('test-jwt-token-123');
      expect(localStorage.getItem('gatherly_token')).toBe('test-jwt-token-123');
    });

    it('clears token from localStorage when set to null or on logout', () => {
      api.setToken('test-token');
      expect(api.getToken()).toBe('test-token');

      api.logout();
      expect(api.getToken()).toBeNull();
      expect(localStorage.getItem('gatherly_token')).toBeNull();
    });
  });

  describe('Authentication Requests', () => {
    it('attaches Bearer token to headers when authenticated', async () => {
      api.setToken('secret-token');

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({ id: 1, email: 'alex@example.com', displayName: 'Alex' }),
      });
      globalThis.fetch = mockFetch;

      const user = await api.getMe();
      expect(user.email).toBe('alex@example.com');
      expect(mockFetch).toHaveBeenCalledTimes(1);

      const requestOptions = mockFetch.mock.calls[0][1];
      expect(requestOptions.headers['Authorization']).toBe('Bearer secret-token');
      expect(requestOptions.headers['Content-Type']).toBe('application/json');
    });

    it('sets token automatically upon successful login', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
          token: 'logged-in-jwt-token',
          user: { id: 2, email: 'user@example.com', displayName: 'User' },
        }),
      });
      globalThis.fetch = mockFetch;

      const response = await api.login('user@example.com', 'password123');
      expect(response.token).toBe('logged-in-jwt-token');
      expect(api.getToken()).toBe('logged-in-jwt-token');
      expect(localStorage.getItem('gatherly_token')).toBe('logged-in-jwt-token');
    });
  });

  describe('Error Handling', () => {
    it('extracts fieldErrors if provided by backend validation', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({
          error: 'Validation Failed',
          fieldErrors: {
            name: 'Name cannot be blank',
            totalBudget: 'Budget must be non-negative',
          },
        }),
      });
      globalThis.fetch = mockFetch;

      await expect(api.createEvent({})).rejects.toThrow(
        'Name cannot be blank, Budget must be non-negative'
      );
    });

    it('falls back to message or status error message on failure', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: async () => ({
          message: 'You do not own this event',
        }),
      });
      globalThis.fetch = mockFetch;

      await expect(api.getEvent(999)).rejects.toThrow('You do not own this event');
    });

    it('handles non-json or network error response status cleanly', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 502,
        json: async () => {
          throw new Error('Not JSON');
        },
      });
      globalThis.fetch = mockFetch;

      await expect(api.getEvent(1)).rejects.toThrow('Request failed with status 502');
    });
  });

  describe('Budget & Vendor Operations', () => {
    it('appends budgetItemId query parameter when booking vendor with linked budget item', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
          id: 5,
          name: 'Floral Design',
          bookingStatus: 'BOOKED',
        }),
      });
      globalThis.fetch = mockFetch;

      await api.bookVendor(10, 5, 42);
      expect(mockFetch).toHaveBeenCalledTimes(1);
      const url = mockFetch.mock.calls[0][0];
      expect(url).toContain('/events/10/vendors/5/book?budgetItemId=42');
    });
  });
});
