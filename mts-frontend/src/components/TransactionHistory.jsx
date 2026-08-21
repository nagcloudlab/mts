import { useState, useEffect } from 'react';
import { fetchTransactionsByAccount } from '../services/api';

function TransactionHistory({ accounts, refreshTrigger }) {
  const [selectedAccount, setSelectedAccount] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (selectedAccount) {
      loadTransactions(selectedAccount);
    }
  }, [selectedAccount, refreshTrigger]);

  async function loadTransactions(accountNumber) {
    setLoading(true);
    setError('');
    try {
      const data = await fetchTransactionsByAccount(accountNumber);
      setTransactions(data);
    } catch (err) {
      setError(err.message);
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  return (
    <div className="card">
      <div className="card-header">Transaction History</div>

      <div className="form-group">
        <label htmlFor="historyAccount">Select Account</label>
        <select
          id="historyAccount"
          value={selectedAccount}
          onChange={(e) => setSelectedAccount(e.target.value)}
        >
          <option value="">-- Select Account --</option>
          {accounts.map((acc) => (
            <option key={acc.accountNumber} value={acc.accountNumber}>
              {acc.accountNumber} - {acc.ownerName}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && <div className="loading">Loading transactions...</div>}

      {!loading && selectedAccount && transactions.length === 0 && (
        <p style={{ color: 'var(--gray-500)', padding: '20px 0' }}>No transactions found.</p>
      )}

      {transactions.length > 0 && (
        <div style={{ overflowX: 'auto' }}>
          <table className="txn-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>From</th>
                <th>To</th>
                <th>Amount</th>
                <th>Mode</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((txn) => (
                <tr key={txn.referenceId}>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                    {txn.referenceId.substring(0, 8)}...
                  </td>
                  <td>{txn.fromAccountNumber}</td>
                  <td>{txn.toAccountNumber}</td>
                  <td style={{ fontWeight: 600 }}>
                    {txn.fromAccountNumber === selectedAccount ? '-' : '+'}
                    INR {Number(txn.amount).toLocaleString('en-IN')}
                  </td>
                  <td>{txn.transferMode}</td>
                  <td>
                    <span className={`status-badge status-${txn.status}`}>
                      {txn.status}
                    </span>
                  </td>
                  <td>{formatDate(txn.timestamp)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default TransactionHistory;
