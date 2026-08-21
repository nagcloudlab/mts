import { useState } from 'react';
import { transferFunds } from '../services/api';

const TRANSFER_MODES = ['UPI', 'NEFT', 'IMPS', 'RTGS'];

const initialFormState = {
  fromAccountNumber: '',
  toAccountNumber: '',
  amount: '',
  transferMode: 'UPI',
  description: '',
};

function TransferForm({ accounts, onTransferComplete }) {
  const [form, setForm] = useState(initialFormState);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Clear field error on change
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  }

  function validate() {
    const newErrors = {};
    if (!form.fromAccountNumber) newErrors.fromAccountNumber = 'Select source account';
    if (!form.toAccountNumber) newErrors.toAccountNumber = 'Select destination account';
    if (form.fromAccountNumber && form.fromAccountNumber === form.toAccountNumber) {
      newErrors.toAccountNumber = 'Cannot transfer to the same account';
    }
    if (!form.amount || Number(form.amount) < 1) {
      newErrors.amount = 'Minimum amount is 1.00';
    }
    if (Number(form.amount) > 1000000) {
      newErrors.amount = 'Maximum amount is 10,00,000.00';
    }
    return newErrors;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setResult(null);

    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    try {
      const response = await transferFunds({
        ...form,
        amount: Number(form.amount),
      });
      setResult({ type: 'success', data: response });
      setForm(initialFormState);
      if (onTransferComplete) onTransferComplete();
    } catch (err) {
      setResult({
        type: 'error',
        message: err.message || 'Transfer failed',
        details: err.details || [],
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card">
      <div className="card-header">Fund Transfer</div>

      {result?.type === 'success' && (
        <div className="alert alert-success">
          Transfer successful! Reference: <strong>{result.data.referenceId}</strong>
          {' | '}Amount: INR {Number(result.data.amount).toLocaleString('en-IN')}
        </div>
      )}

      {result?.type === 'error' && (
        <div className="alert alert-error">
          {result.message}
          {result.details.length > 0 && (
            <ul>{result.details.map((d, i) => <li key={i}>{d}</li>)}</ul>
          )}
        </div>
      )}

      <form onSubmit={handleSubmit} className="transfer-form">
        <div className="form-group">
          <label htmlFor="fromAccountNumber">From Account</label>
          <select
            id="fromAccountNumber"
            name="fromAccountNumber"
            value={form.fromAccountNumber}
            onChange={handleChange}
          >
            <option value="">-- Select Account --</option>
            {accounts.map((acc) => (
              <option key={acc.accountNumber} value={acc.accountNumber}>
                {acc.accountNumber} - {acc.ownerName} (INR {Number(acc.balance).toLocaleString('en-IN')})
              </option>
            ))}
          </select>
          {errors.fromAccountNumber && <div className="error-text">{errors.fromAccountNumber}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="toAccountNumber">To Account</label>
          <select
            id="toAccountNumber"
            name="toAccountNumber"
            value={form.toAccountNumber}
            onChange={handleChange}
          >
            <option value="">-- Select Account --</option>
            {accounts.map((acc) => (
              <option key={acc.accountNumber} value={acc.accountNumber}>
                {acc.accountNumber} - {acc.ownerName}
              </option>
            ))}
          </select>
          {errors.toAccountNumber && <div className="error-text">{errors.toAccountNumber}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="amount">Amount (INR)</label>
          <input
            id="amount"
            name="amount"
            type="number"
            step="0.01"
            min="1"
            max="1000000"
            placeholder="Enter amount"
            value={form.amount}
            onChange={handleChange}
          />
          {errors.amount && <div className="error-text">{errors.amount}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="transferMode">Transfer Mode</label>
          <select
            id="transferMode"
            name="transferMode"
            value={form.transferMode}
            onChange={handleChange}
          >
            {TRANSFER_MODES.map((mode) => (
              <option key={mode} value={mode}>{mode}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="description">Description (optional)</label>
          <textarea
            id="description"
            name="description"
            rows="2"
            maxLength="255"
            placeholder="Payment for..."
            value={form.description}
            onChange={handleChange}
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Processing...' : 'Transfer Funds'}
        </button>
      </form>
    </div>
  );
}

export default TransferForm;
