import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchDashboardStats, fetchAllTransactions } from '../services/api';

function StatCard({ label, value, sub, color }) {
  return (
    <div className={`stat-card stat-${color}`}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [recentTxns, setRecentTxns] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [statsData, txnData] = await Promise.all([
          fetchDashboardStats(),
          fetchAllTransactions(),
        ]);
        setStats(statsData);
        setRecentTxns(txnData.slice(0, 5));
      } catch {
        // stats may not load if no transactions yet
        setStats(null);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  function fmt(num) {
    return Number(num || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
    });
  }

  if (loading) return <div className="loading">Loading dashboard...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Dashboard</h2>
        <Link to="/transfer" className="btn btn-primary">New Transfer</Link>
      </div>

      <div className="stats-grid">
        <StatCard label="Total Accounts" value={stats?.totalAccounts || 0} sub={`${stats?.activeAccounts || 0} active`} color="blue" />
        <StatCard label="Total Balance" value={`INR ${fmt(stats?.totalBalance)}`} color="green" />
        <StatCard label="Transactions" value={stats?.totalTransactions || 0} sub={`${stats?.successfulTransactions || 0} success / ${stats?.failedTransactions || 0} failed`} color="purple" />
        <StatCard label="Total Transferred" value={`INR ${fmt(stats?.totalTransferred)}`} color="orange" />
      </div>

      <div className="content-grid">
        <div className="card">
          <div className="card-header">
            <span>Recent Transactions</span>
            <Link to="/transactions" className="card-link">View All</Link>
          </div>
          {recentTxns.length === 0 ? (
            <div className="empty-state">
              <p>No transactions yet</p>
              <Link to="/transfer" className="btn btn-primary btn-sm">Make your first transfer</Link>
            </div>
          ) : (
            <table className="txn-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>From</th>
                  <th>To</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {recentTxns.map((txn) => (
                  <tr key={txn.referenceId}>
                    <td className="mono">{txn.referenceId.substring(0, 8)}...</td>
                    <td>{txn.fromAccountNumber}</td>
                    <td>{txn.toAccountNumber}</td>
                    <td className="amount">INR {fmt(txn.amount)}</td>
                    <td><span className={`status-badge status-${txn.status}`}>{txn.status}</span></td>
                    <td>{formatDate(txn.timestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <div className="card-header">Quick Actions</div>
          <div className="quick-actions">
            <Link to="/transfer" className="action-card">
              <div className="action-icon">&#8644;</div>
              <div>
                <strong>Fund Transfer</strong>
                <p>Send money via UPI, NEFT, IMPS, RTGS</p>
              </div>
            </Link>
            <Link to="/accounts" className="action-card">
              <div className="action-icon">&#9776;</div>
              <div>
                <strong>View Accounts</strong>
                <p>Check balances and account details</p>
              </div>
            </Link>
            <Link to="/transactions" className="action-card">
              <div className="action-icon">&#128221;</div>
              <div>
                <strong>Transaction History</strong>
                <p>View all past transactions</p>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;
