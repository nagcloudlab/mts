# MTS Frontend

React SPA for the Money Transfer System.

## Tech Stack

- React 18, React Router 6
- Vite (dev server + build)
- Jest + React Testing Library

## Run

```bash
npm install
npm run dev        # Dev server at http://localhost:5173
npm run build      # Production build
npm test           # Run tests
```

## Pages

| Route | Page | Features |
|-------|------|----------|
| `/` | Dashboard | Stats cards, recent transactions, quick actions |
| `/accounts` | Accounts | Search, filter, account cards with balances |
| `/accounts/:id` | Account Detail | Info, credit/debit summary, transaction history |
| `/transfer` | Fund Transfer | Form with validation, mode selector, balance hint |
| `/transactions` | Transactions | Status filter tabs, search, full table |

## Project Structure

```
src/
├── components/       # Layout, AccountCard (shared components)
├── pages/            # DashboardPage, AccountsPage, AccountDetailPage,
│                     # TransferPage, TransactionsPage
├── services/         # api.js (Fetch wrapper for all API calls)
├── __tests__/        # Jest tests (TransferForm, API service)
├── __mocks__/        # Style mock for Jest
├── App.jsx           # Router setup
├── main.jsx          # Entry point
└── index.css         # Global styles (CSS variables, grid, responsive)
```

## API Proxy

Vite proxies `/api/*` to `http://localhost:8080` (Spring Boot backend).

Notification service proxied via `/notification-api/*` to `http://localhost:3000`.

## Key Patterns

- **React Router** — `BrowserRouter` with nested `<Outlet>` layout
- **Component composition** — Layout wraps all pages
- **API service layer** — Centralized fetch with error handling
- **Client-side validation** — Form validation before API call
- **Conditional rendering** — Loading, error, empty states
- **CSS Grid + Flexbox** — Responsive layout with CSS variables
