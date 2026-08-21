import { useState, useEffect } from 'react';
import { fetchAllTransactions } from '../services/api';

function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const data = await fetchAllTransactions();
        setTransactions(data);
      } catch {
        // handled
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  function fmt(num) {
    return Number(num).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  }

  const filtered = transactions.filter((txn) => {
    const matchesFilter = filter === 'ALL' || txn.status === filter;
    const matchesSearch = search === '' ||
      txn.referenceId.toLowerCase().includes(search.toLowerCase()) ||
      txn.fromAccountNumber.toLowerCase().includes(search.toLowerCase()) ||
      txn.toAccountNumber.toLowerCase().includes(search.toLowerCase()) ||
      (txn.description || '').toLowerCase().includes(search.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const statusCounts = {
    ALL: transactions.length,
    SUCCESS: transactions.filter((t) => t.status === 'SUCCESS').length,
    FAILED: transactions.filter((t) => t.status === 'FAILED').length,
    PENDING: transactions.filter((t) => t.status === 'PENDING').length,
  };

  if (loading) return <div className="loading">Loading transactions...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Transactions ({transactions.length})</h2>
      </div>

      <div className="card">
        <div className="filter-bar">
          <div className="filter-tabs">
            {['ALL', 'SUCCESS', 'FAILED', 'PENDING'].map((s) => (
              <button
                key={s}
                className={`filter-tab ${filter === s ? 'filter-active' : ''} ${s !== 'ALL' ? `filter-${s.toLowerCase()}` : ''}`}
                onClick={() => setFilter(s)}
              >
                {s} ({statusCounts[s]})
              </button>
            ))}
          </div>
          <div className="search-box">
            <input
              type="text"
              placeholder="Search by reference, account, description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {filtered.length === 0 ? (
          <div className="empty-state">
            {transactions.length === 0
              ? 'No transactions yet. Make your first transfer!'
              : 'No transactions match your filters.'}
          </div>
        ) : (
          <div className="table-wrap">
            <table className="txn-table">
              <thead>
                <tr>
                  <th>Reference ID</th>
                  <th>From</th>
                  <th>To</th>
                  <th>Amount</th>
                  <th>Mode</th>
                  <th>Status</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((txn) => (
                  <tr key={txn.referenceId}>
                    <td className="mono">{txn.referenceId.substring(0, 12)}...</td>
                    <td><strong>{txn.fromAccountNumber}</strong></td>
                    <td><strong>{txn.toAccountNumber}</strong></td>
                    <td className="amount">INR {fmt(txn.amount)}</td>
                    <td><span className="mode-tag">{txn.transferMode}</span></td>
                    <td><span className={`status-badge status-${txn.status}`}>{txn.status}</span></td>
                    <td>{txn.description || '-'}</td>
                    <td className="date-cell">{formatDate(txn.timestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default TransactionsPage;
