import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchAccountByNumber, fetchTransactionsByAccount } from '../services/api';

function AccountDetailPage() {
  const { accountNumber } = useParams();
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const [accData, txnData] = await Promise.all([
          fetchAccountByNumber(accountNumber),
          fetchTransactionsByAccount(accountNumber),
        ]);
        setAccount(accData);
        setTransactions(txnData);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [accountNumber]);

  function fmt(num) {
    return Number(num).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  }

  if (loading) return <div className="loading">Loading account...</div>;
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!account) return null;

  const credited = transactions.filter((t) => t.toAccountNumber === accountNumber && t.status === 'SUCCESS');
  const debited = transactions.filter((t) => t.fromAccountNumber === accountNumber && t.status === 'SUCCESS');
  const totalIn = credited.reduce((s, t) => s + Number(t.amount), 0);
  const totalOut = debited.reduce((s, t) => s + Number(t.amount), 0);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <Link to="/accounts" className="breadcrumb">&#8592; All Accounts</Link>
          <h2>Account: {account.accountNumber}</h2>
        </div>
        <Link to={`/transfer?from=${account.accountNumber}`} className="btn btn-primary">Transfer from this Account</Link>
      </div>

      <div className="detail-grid">
        <div className="card detail-card">
          <div className="card-header">Account Information</div>
          <div className="detail-rows">
            <div className="detail-row">
              <span className="detail-label">Account Number</span>
              <span className="detail-value mono">{account.accountNumber}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Account Holder</span>
              <span className="detail-value">{account.ownerName}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Account Type</span>
              <span className="detail-value"><span className={`account-type-badge type-${account.accountType}`}>{account.accountType}</span></span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Currency</span>
              <span className="detail-value">{account.currency}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Status</span>
              <span className="detail-value">
                <span className={`status-dot ${account.isActive ? 'dot-active' : 'dot-inactive'}`}>
                  {account.isActive ? 'Active' : 'Inactive'}
                </span>
              </span>
            </div>
            <div className="detail-row balance-row">
              <span className="detail-label">Current Balance</span>
              <span className="detail-value balance-big">INR {fmt(account.balance)}</span>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header">Account Summary</div>
          <div className="summary-cards">
            <div className="summary-item summary-in">
              <div className="summary-val">INR {fmt(totalIn)}</div>
              <div className="summary-label">Total Credited ({credited.length})</div>
            </div>
            <div className="summary-item summary-out">
              <div className="summary-val">INR {fmt(totalOut)}</div>
              <div className="summary-label">Total Debited ({debited.length})</div>
            </div>
            <div className="summary-item summary-count">
              <div className="summary-val">{transactions.length}</div>
              <div className="summary-label">Total Transactions</div>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">Transaction History</div>
        {transactions.length === 0 ? (
          <div className="empty-state">No transactions for this account yet.</div>
        ) : (
          <div className="table-wrap">
            <table className="txn-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Counterparty</th>
                  <th>Amount</th>
                  <th>Mode</th>
                  <th>Status</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((txn) => {
                  const isDebit = txn.fromAccountNumber === accountNumber;
                  return (
                    <tr key={txn.referenceId}>
                      <td className="mono">{txn.referenceId.substring(0, 8)}...</td>
                      <td><span className={`type-tag ${isDebit ? 'type-debit' : 'type-credit'}`}>{isDebit ? 'DEBIT' : 'CREDIT'}</span></td>
                      <td>{isDebit ? txn.toAccountNumber : txn.fromAccountNumber}</td>
                      <td className={`amount ${isDebit ? 'amount-debit' : 'amount-credit'}`}>
                        {isDebit ? '-' : '+'}INR {fmt(txn.amount)}
                      </td>
                      <td>{txn.transferMode}</td>
                      <td><span className={`status-badge status-${txn.status}`}>{txn.status}</span></td>
                      <td>{txn.description || '-'}</td>
                      <td>{formatDate(txn.timestamp)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default AccountDetailPage;
