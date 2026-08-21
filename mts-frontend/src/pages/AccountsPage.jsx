import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchAccounts } from '../services/api';

function AccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const data = await fetchAccounts();
        setAccounts(data);
      } catch {
        // handled
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const filtered = accounts.filter(
    (acc) =>
      acc.accountNumber.toLowerCase().includes(search.toLowerCase()) ||
      acc.ownerName.toLowerCase().includes(search.toLowerCase())
  );

  const totalBalance = accounts.reduce((sum, acc) => sum + Number(acc.balance), 0);

  function fmt(num) {
    return Number(num).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  if (loading) return <div className="loading">Loading accounts...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Accounts ({accounts.length})</h2>
        <div className="page-header-right">
          <div className="search-box">
            <input
              type="text"
              placeholder="Search by name or account..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="stats-grid stats-grid-compact">
        <div className="stat-card stat-green">
          <div className="stat-value">INR {fmt(totalBalance)}</div>
          <div className="stat-label">Total Balance</div>
        </div>
        <div className="stat-card stat-blue">
          <div className="stat-value">{accounts.filter((a) => a.accountType === 'SAVINGS').length}</div>
          <div className="stat-label">Savings Accounts</div>
        </div>
        <div className="stat-card stat-purple">
          <div className="stat-value">{accounts.filter((a) => a.accountType === 'CURRENT').length}</div>
          <div className="stat-label">Current Accounts</div>
        </div>
      </div>

      <div className="accounts-grid">
        {filtered.map((account) => (
          <Link to={`/accounts/${account.accountNumber}`} key={account.accountNumber} className="account-card-link">
            <div className={`account-card ${!account.isActive ? 'account-inactive' : ''}`}>
              <div className="account-card-top">
                <span className="account-number">{account.accountNumber}</span>
                <span className={`account-type-badge type-${account.accountType}`}>{account.accountType}</span>
              </div>
              <div className="account-owner">{account.ownerName}</div>
              <div className="account-balance">
                <span className="currency">INR </span>
                {fmt(account.balance)}
              </div>
              <div className="account-card-footer">
                <span className={`status-dot ${account.isActive ? 'dot-active' : 'dot-inactive'}`}>
                  {account.isActive ? 'Active' : 'Inactive'}
                </span>
                <span className="view-link">View Details &#8594;</span>
              </div>
            </div>
          </Link>
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="empty-state">No accounts match your search.</div>
      )}
    </div>
  );
}

export default AccountsPage;
