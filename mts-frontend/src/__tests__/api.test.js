import { fetchAccounts, transferFunds, ApiError } from '../services/api';

/**
 * QC - API Service Layer Tests
 *
 * Tests the API abstraction with mocked fetch
 * Validates: request formation, response parsing, error handling
 */

// Mock global fetch
global.fetch = jest.fn();

describe('API Service', () => {
  beforeEach(() => {
    fetch.mockClear();
  });

  describe('fetchAccounts', () => {
    test('returns accounts on success', async () => {
      const mockData = [
        { accountNumber: 'ACC001', balance: 50000 },
        { accountNumber: 'ACC002', balance: 20000 },
      ];

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockData),
      });

      const result = await fetchAccounts();

      expect(fetch).toHaveBeenCalledWith('/api/accounts');
      expect(result).toEqual(mockData);
    });

    test('throws ApiError on failure', async () => {
      fetch.mockResolvedValue({
        ok: false,
        status: 500,
        json: () => Promise.resolve({ message: 'Server error' }),
      });

      await expect(fetchAccounts()).rejects.toThrow('Server error');
    });
  });

  describe('transferFunds', () => {
    test('sends POST with correct body', async () => {
      const request = {
        fromAccountNumber: 'ACC001',
        toAccountNumber: 'ACC002',
        amount: 5000,
        transferMode: 'UPI',
      };

      const mockResponse = { referenceId: 'ref-123', status: 'SUCCESS' };

      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const result = await transferFunds(request);

      expect(fetch).toHaveBeenCalledWith('/api/transactions/transfer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });
      expect(result.referenceId).toBe('ref-123');
    });

    test('throws with details on validation error', async () => {
      fetch.mockResolvedValue({
        ok: false,
        status: 400,
        json: () => Promise.resolve({
          message: 'Validation failed',
          details: ['amount: Minimum transfer amount is 1.00'],
        }),
      });

      try {
        await transferFunds({});
        fail('Should have thrown');
      } catch (err) {
        expect(err.message).toBe('Validation failed');
        expect(err.details).toContain('amount: Minimum transfer amount is 1.00');
      }
    });
  });
});
