const API_BASE = '/api';

class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

async function handleResponse(response) {
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok) {
    throw new ApiError(
      response.status,
      data.message || 'Something went wrong',
      data.details || []
    );
  }
  return data;
}

// Account APIs
export async function fetchAccounts() {
  const response = await fetch(`${API_BASE}/accounts`);
  return handleResponse(response);
}

export async function fetchAccountByNumber(accountNumber) {
  const response = await fetch(`${API_BASE}/accounts/${accountNumber}`);
  return handleResponse(response);
}

export async function fetchAccountsByUser(username) {
  const response = await fetch(`${API_BASE}/accounts/user/${username}`);
  return handleResponse(response);
}

// Transaction APIs
export async function transferFunds(transferRequest) {
  const response = await fetch(`${API_BASE}/transactions/transfer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(transferRequest),
  });
  return handleResponse(response);
}

export async function fetchAllTransactions() {
  const response = await fetch(`${API_BASE}/transactions`);
  return handleResponse(response);
}

export async function fetchTransactionByRef(referenceId) {
  const response = await fetch(`${API_BASE}/transactions/${referenceId}`);
  return handleResponse(response);
}

export async function fetchTransactionsByAccount(accountNumber) {
  const response = await fetch(`${API_BASE}/transactions/account/${accountNumber}`);
  return handleResponse(response);
}

export async function fetchTransactionsByUser(username) {
  const response = await fetch(`${API_BASE}/transactions/user/${username}`);
  return handleResponse(response);
}

// Stats
export async function fetchDashboardStats() {
  const response = await fetch(`${API_BASE}/transactions/stats`);
  return handleResponse(response);
}

// Notification Service
export async function fetchNotificationStats() {
  const response = await fetch('/notification-api/notifications/stats');
  return handleResponse(response);
}

export { ApiError };
