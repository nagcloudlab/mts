# MTS Notification Service

Node.js Express microservice for transaction notifications and audit logging.

## Tech Stack

- Node.js 18+, Express 4
- Jest + Supertest

## Run

```bash
npm install
npm start          # Production at http://localhost:3000
npm run dev        # Dev with auto-reload (--watch)
npm test           # Run tests
```

## Features

- **Dashboard UI** at `http://localhost:3000` — Live audit log with auto-refresh (5s)
- **REST API** for receiving notifications from Spring Boot backend
- **In-memory audit log** — Stores all transaction notifications
- **Statistics** — Total, successful, failed notification counts

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Dashboard UI (HTML) |
| GET | `/health` | Health check |
| POST | `/api/notifications/transaction` | Log a transaction notification |
| GET | `/api/notifications/audit` | Full audit log |
| GET | `/api/notifications/audit/:referenceId` | Audit entry by reference |
| GET | `/api/notifications/stats` | Notification statistics |

## Project Structure

```
src/
├── server.js                  # Express app, middleware, dashboard UI
├── routes/
│   └── notification.js        # Notification REST routes
└── services/
    └── notificationService.js # In-memory audit log + stats

__tests__/
└── notification.test.js       # Supertest API tests
```

## How It Works

1. Spring Boot backend calls `POST /api/notifications/transaction` after every transfer
2. This service logs the notification in-memory and prints to console
3. The dashboard UI at `/` shows all logged notifications in real-time
4. The call is **non-blocking** — if this service is down, transfers still succeed

## Key Patterns

- **Middleware** — Request logging, CORS, JSON parsing
- **Error handling** — 404 handler, global error handler
- **Test isolation** — `NODE_ENV=test` prevents server startup during tests
- **Server-side HTML** — Dashboard rendered as template string (no frontend framework)
