const request = require('supertest');
const app = require('../src/server');

/**
 * QC - Node.js API Tests using Supertest
 *
 * Tests Express routes: status codes, response structure, edge cases
 */
describe('Notification Service API', () => {

  describe('GET /health', () => {
    test('returns health status', async () => {
      const res = await request(app).get('/health');

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('UP');
      expect(res.body.service).toBe('mts-notification-service');
    });
  });

  describe('POST /api/notifications/transaction', () => {
    test('logs transaction notification - 201', async () => {
      const transaction = {
        referenceId: 'test-ref-001',
        fromAccountNumber: 'ACC001',
        toAccountNumber: 'ACC002',
        amount: 5000,
        status: 'SUCCESS',
        transferMode: 'UPI',
      };

      const res = await request(app)
        .post('/api/notifications/transaction')
        .send(transaction);

      expect(res.status).toBe(201);
      expect(res.body.message).toBe('Notification sent');
      expect(res.body.entry.referenceId).toBe('test-ref-001');
      expect(res.body.entry.channel).toBe('SMS');
    });

    test('returns 400 when referenceId is missing', async () => {
      const res = await request(app)
        .post('/api/notifications/transaction')
        .send({ amount: 100 });

      expect(res.status).toBe(400);
      expect(res.body.error).toBe('referenceId is required');
    });
  });

  describe('GET /api/notifications/audit', () => {
    test('returns audit log', async () => {
      // First create a notification
      await request(app)
        .post('/api/notifications/transaction')
        .send({ referenceId: 'audit-test-001', status: 'SUCCESS', amount: 1000 });

      const res = await request(app).get('/api/notifications/audit');

      expect(res.status).toBe(200);
      expect(res.body.count).toBeGreaterThanOrEqual(1);
      expect(Array.isArray(res.body.entries)).toBe(true);
    });
  });

  describe('GET /api/notifications/audit/:referenceId', () => {
    test('returns specific audit entry', async () => {
      await request(app)
        .post('/api/notifications/transaction')
        .send({ referenceId: 'specific-ref-001', status: 'SUCCESS', amount: 2000 });

      const res = await request(app).get('/api/notifications/audit/specific-ref-001');

      expect(res.status).toBe(200);
      expect(res.body.referenceId).toBe('specific-ref-001');
    });

    test('returns 404 for non-existent reference', async () => {
      const res = await request(app).get('/api/notifications/audit/nonexistent');

      expect(res.status).toBe(404);
    });
  });

  describe('GET /api/notifications/stats', () => {
    test('returns notification statistics', async () => {
      const res = await request(app).get('/api/notifications/stats');

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('total');
      expect(res.body).toHaveProperty('successful');
      expect(res.body).toHaveProperty('failed');
    });
  });

  describe('404 handler', () => {
    test('returns 404 for unknown routes', async () => {
      const res = await request(app).get('/unknown');
      expect(res.status).toBe(404);
    });
  });
});
