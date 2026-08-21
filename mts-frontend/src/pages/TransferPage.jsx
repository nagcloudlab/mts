import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchAccounts, transferFunds } from '../services/api';

const TRANSFER_MODES = [
  { value: 'UPI', label: 'UPI', desc: 'Instant, 24x7' },
  { value: 'IMPS', label: 'IMPS', desc: 'Instant, up to 5L' },
  { value: 'NEFT', label: 'NEFT', desc: 'Batch, no limit' },
  { value: 'RTGS', label: 'RTGS', desc: 'Real-time, min 2L' },
];

const initialForm = {
  fromAccountNumber: '',
  toAccountNumber: '',
  amount: '',
  transferMode: 'UPI',
  description: '',
};

function TransferPage() {
  const [searchParams] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({ ...initialForm, fromAccountNumber: searchParams.get('from') || '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [pageLoading, setPageLoading] = useState(true);

  useEffect(() => {
    fetchAccounts().then(setAccounts).finally(() => setPageLoading(false));
  }, []);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  }

  function validate() {
    const e = {};
    if (!form.fromAccountNumber) e.fromAccountNumber = 'Select source account';
    if (!form.toAccountNumber) e.toAccountNumber = 'Select destination account';
    if (form.fromAccountNumber && form.fromAccountNumber === form.toAccountNumber) {
      e.toAccountNumber = 'Cannot transfer to the same account';
    }
    if (!form.amount || Number(form.amount) < 1) e.amount = 'Minimum amount is INR 1.00';
    if (Number(form.amount) > 1000000) e.amount = 'Maximum amount is INR 10,00,000';
    return e;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setResult(null);
    const v = validate();
    if (Object.keys(v).length > 0) { setErrors(v); return; }

    setLoading(true);
    try {
      const response = await transferFunds({ ...form, amount: Number(form.amount) });
      setResult({ type: 'success', data: response });
      setForm(initialForm);
      // Refresh accounts to reflect new balances
      fetchAccounts().then(setAccounts);
    } catch (err) {
      setResult({ type: 'error', message: err.message || 'Transfer failed', details: err.details || [] });
    } finally {
      setLoading(false);
    }
  }

  function fmt(num) {
    return Number(num).toLocaleString('en-IN', { minimumFractionDigits: 2 });
  }

  const selectedFrom = accounts.find((a) => a.accountNumber === form.fromAccountNumber);

  if (pageLoading) return <div className="loading">Loading...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Fund Transfer</h2>
      </div>

      <div className="transfer-layout">
        <div className="card transfer-card">
          {result?.type === 'success' && (
            <div className="alert alert-success">
              <div className="alert-title">Transfer Successful!</div>
              <div className="alert-body">
                <p>Reference: <strong className="mono">{result.data.referenceId}</strong></p>
                <p>Amount: <strong>INR {fmt(result.data.amount)}</strong> via {result.data.transferMode}</p>
                <p>{result.data.fromAccountNumber} &#8594; {result.data.toAccountNumber}</p>
              </div>
              <Link to="/transactions" className="btn btn-sm btn-outline">View Transactions</Link>
            </div>
          )}

          {result?.type === 'error' && (
            <div className="alert alert-error">
              <div className="alert-title">Transfer Failed</div>
              <p>{result.message}</p>
              {result.details.length > 0 && (
                <ul className="alert-list">{result.details.map((d, i) => <li key={i}>{d}</li>)}</ul>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="fromAccountNumber">From Account</label>
              <select id="fromAccountNumber" name="fromAccountNumber" value={form.fromAccountNumber} onChange={handleChange}>
                <option value="">-- Select Source Account --</option>
                {accounts.filter((a) => a.isActive).map((acc) => (
                  <option key={acc.accountNumber} value={acc.accountNumber}>
                    {acc.accountNumber} - {acc.ownerName} ({acc.accountType}) | INR {fmt(acc.balance)}
                  </option>
                ))}
              </select>
              {errors.fromAccountNumber && <div className="error-text">{errors.fromAccountNumber}</div>}
            </div>

            {selectedFrom && (
              <div className="balance-hint">
                Available Balance: <strong>INR {fmt(selectedFrom.balance)}</strong>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="toAccountNumber">To Account</label>
              <select id="toAccountNumber" name="toAccountNumber" value={form.toAccountNumber} onChange={handleChange}>
                <option value="">-- Select Destination Account --</option>
                {accounts.filter((a) => a.isActive && a.accountNumber !== form.fromAccountNumber).map((acc) => (
                  <option key={acc.accountNumber} value={acc.accountNumber}>
                    {acc.accountNumber} - {acc.ownerName} ({acc.accountType})
                  </option>
                ))}
              </select>
              {errors.toAccountNumber && <div className="error-text">{errors.toAccountNumber}</div>}
            </div>

            <div className="form-group">
              <label htmlFor="amount">Amount (INR)</label>
              <input id="amount" name="amount" type="number" step="0.01" min="1" max="1000000" placeholder="0.00" value={form.amount} onChange={handleChange} />
              {errors.amount && <div className="error-text">{errors.amount}</div>}
            </div>

            <div className="form-group">
              <label>Transfer Mode</label>
              <div className="mode-grid">
                {TRANSFER_MODES.map((mode) => (
                  <label key={mode.value} className={`mode-option ${form.transferMode === mode.value ? 'mode-selected' : ''}`}>
                    <input type="radio" name="transferMode" value={mode.value} checked={form.transferMode === mode.value} onChange={handleChange} />
                    <strong>{mode.label}</strong>
                    <span>{mode.desc}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="description">Description (optional)</label>
              <textarea id="description" name="description" rows="2" maxLength="255" placeholder="Payment for..." value={form.description} onChange={handleChange} />
            </div>

            <button type="submit" className="btn btn-primary btn-lg" disabled={loading}>
              {loading ? 'Processing Transfer...' : 'Transfer Funds'}
            </button>
          </form>
        </div>

        <div className="transfer-sidebar">
          <div className="card">
            <div className="card-header">Transfer Limits</div>
            <div className="detail-rows">
              <div className="detail-row"><span className="detail-label">UPI</span><span className="detail-value">Up to INR 1,00,000</span></div>
              <div className="detail-row"><span className="detail-label">IMPS</span><span className="detail-value">Up to INR 5,00,000</span></div>
              <div className="detail-row"><span className="detail-label">NEFT</span><span className="detail-value">No upper limit</span></div>
              <div className="detail-row"><span className="detail-label">RTGS</span><span className="detail-value">Min INR 2,00,000</span></div>
            </div>
          </div>
          <div className="card">
            <div className="card-header">Important Notes</div>
            <ul className="notes-list">
              <li>Transfers are processed instantly for UPI and IMPS</li>
              <li>NEFT transfers are processed in batches</li>
              <li>Ensure sufficient balance before transfer</li>
              <li>Transaction reference ID is your proof of transfer</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default TransferPage;
