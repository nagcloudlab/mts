import { useState, useEffect } from 'react';
import { fetchAccounts } from '../services/api';
import AccountCard from './AccountCard';
import TransferForm from './TransferForm';
import TransactionHistory from './TransactionHistory';

function Dashboard() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    loadAccounts();
  }, [refreshTrigger]);

  async function loadAccounts() {
    setLoading(true);
    try {
      const data = await fetchAccounts();
      setAccounts(data);
      setError('');
    } catch (err) {
      setError('Failed to load accounts. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }

  function handleTransferComplete() {
    setRefreshTrigger((prev) => prev + 1);
  }

  if (loading) return <div className="loading">Loading accounts...</div>;
  if (error) return <div className="alert alert-error">{error}</div>;

  return (
    <div>
      <h2 style={{ marginBottom: 20, fontSize: '1.3rem' }}>Accounts Overview</h2>
      <div className="accounts-grid">
        {accounts.map((account) => (
          <AccountCard key={account.accountNumber} account={account} />
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, alignItems: 'start' }}>
        <TransferForm accounts={accounts} onTransferComplete={handleTransferComplete} />
        <TransactionHistory accounts={accounts} refreshTrigger={refreshTrigger} />
      </div>
    </div>
  );
}

export default Dashboard;
