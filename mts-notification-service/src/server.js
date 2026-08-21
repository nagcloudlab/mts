const express = require('express');
const cors = require('cors');
const notificationRouter = require('./routes/notification');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging middleware
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    console.log(`${req.method} ${req.url} ${res.statusCode} ${duration}ms`);
  });
  next();
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'mts-notification-service', timestamp: new Date().toISOString() });
});

// Dashboard UI
app.get('/', (req, res) => {
  const { getAuditLog, getStats } = require('./services/notificationService');
  const stats = getStats();
  const entries = getAuditLog();

  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MTS Notification Service</title>
  <meta http-equiv="refresh" content="5">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Inter', -apple-system, sans-serif; background: #f0fdf4; color: #1a1a2e; }
    .header { background: linear-gradient(135deg, #065f46, #059669, #10b981); color: #fff; padding: 20px 32px; }
    .header h1 { font-size: 1.4rem; font-weight: 700; }
    .header p { font-size: 0.8rem; opacity: 0.8; margin-top: 4px; }
    .container { max-width: 1100px; margin: 0 auto; padding: 24px 32px; }
    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
    .stat-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); border-left: 4px solid #d1d5db; }
    .stat-card.blue { border-left-color: #3b82f6; }
    .stat-card.green { border-left-color: #10b981; }
    .stat-card.red { border-left-color: #ef4444; }
    .stat-card.purple { border-left-color: #8b5cf6; }
    .stat-val { font-size: 1.8rem; font-weight: 800; }
    .stat-label { font-size: 0.75rem; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 4px; }
    .card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
    .card h2 { font-size: 1rem; font-weight: 600; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid #e5e7eb; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; padding: 8px 12px; font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #9ca3af; border-bottom: 2px solid #e5e7eb; }
    td { padding: 10px 12px; font-size: 0.85rem; border-bottom: 1px solid #f3f4f6; color: #374151; }
    tr:hover { background: #f9fafb; }
    .badge { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 0.7rem; font-weight: 700; }
    .badge-success { background: #d1fae5; color: #065f46; }
    .badge-failed { background: #fee2e2; color: #991b1b; }
    .badge-pending { background: #fef3c7; color: #92400e; }
    .mono { font-family: 'SF Mono', Consolas, monospace; font-size: 0.78rem; }
    .empty { text-align: center; padding: 40px; color: #9ca3af; }
    .live-dot { display: inline-block; width: 8px; height: 8px; background: #10b981; border-radius: 50%; margin-right: 6px; animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    .footer { text-align: center; padding: 16px; font-size: 0.7rem; color: #9ca3af; }
    @media (max-width: 768px) { .stats { grid-template-columns: 1fr 1fr; } .container { padding: 16px; } }
  </style>
</head>
<body>
  <div class="header">
    <h1><span class="live-dot"></span>MTS Notification Service</h1>
    <p>Real-time audit log &amp; notification dashboard | Auto-refreshes every 5 seconds</p>
  </div>
  <div class="container">
    <div class="stats">
      <div class="stat-card blue">
        <div class="stat-val">${stats.total}</div>
        <div class="stat-label">Total Notifications</div>
      </div>
      <div class="stat-card green">
        <div class="stat-val">${stats.successful}</div>
        <div class="stat-label">Successful</div>
      </div>
      <div class="stat-card red">
        <div class="stat-val">${stats.failed}</div>
        <div class="stat-label">Failed</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-val">SMS</div>
        <div class="stat-label">Notification Channel</div>
      </div>
    </div>

    <div class="card">
      <h2>Audit Log (${entries.length} entries)</h2>
      ${entries.length === 0
        ? '<div class="empty">No notifications yet. Make a transfer from the MTS app to see entries here.</div>'
        : `<table>
            <thead>
              <tr>
                <th>#</th>
                <th>Reference ID</th>
                <th>From</th>
                <th>To</th>
                <th>Amount</th>
                <th>Mode</th>
                <th>Status</th>
                <th>Channel</th>
                <th>Notified At</th>
              </tr>
            </thead>
            <tbody>
              ${entries.map((e) => `
                <tr>
                  <td>${e.id}</td>
                  <td class="mono">${e.referenceId ? e.referenceId.substring(0, 12) + '...' : '-'}</td>
                  <td><strong>${e.fromAccount || '-'}</strong></td>
                  <td><strong>${e.toAccount || '-'}</strong></td>
                  <td style="font-weight:700">INR ${Number(e.amount || 0).toLocaleString('en-IN')}</td>
                  <td>${e.transferMode || '-'}</td>
                  <td><span class="badge badge-${(e.status || '').toLowerCase() === 'success' ? 'success' : (e.status || '').toLowerCase() === 'failed' ? 'failed' : 'pending'}">${e.status || '-'}</span></td>
                  <td>${e.channel}</td>
                  <td class="mono">${e.notifiedAt ? new Date(e.notifiedAt).toLocaleString() : '-'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>`
      }
    </div>
  </div>
  <div class="footer">MTS Notification Service v1.0 | Node.js + Express | Port ${PORT}</div>
</body>
</html>`);
});

// Routes
app.use('/api/notifications', notificationRouter);

// 404 handler (skip for HTML pages)
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err.message);
  res.status(500).json({ error: 'Internal server error' });
});

// Only start if not in test mode
if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`Notification service running on port ${PORT}`);
  });
}

module.exports = app;
