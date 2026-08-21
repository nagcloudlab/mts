const express = require('express');
const router = express.Router();
const { logTransaction, getAuditLog, getAuditByReference, getStats } = require('../services/notificationService');

// POST /api/notifications/transaction - Log a transaction notification
router.post('/transaction', (req, res) => {
  const transaction = req.body;

  if (!transaction.referenceId) {
    return res.status(400).json({ error: 'referenceId is required' });
  }

  const entry = logTransaction(transaction);
  res.status(201).json({ message: 'Notification sent', entry });
});

// GET /api/notifications/audit - Get full audit log
router.get('/audit', (req, res) => {
  const log = getAuditLog();
  res.json({ count: log.length, entries: log });
});

// GET /api/notifications/audit/:referenceId - Get audit by reference
router.get('/audit/:referenceId', (req, res) => {
  const entry = getAuditByReference(req.params.referenceId);
  if (!entry) {
    return res.status(404).json({ error: 'Audit entry not found' });
  }
  res.json(entry);
});

// GET /api/notifications/stats - Get notification statistics
router.get('/stats', (req, res) => {
  res.json(getStats());
});

module.exports = router;
