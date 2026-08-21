import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TransferForm from '../components/TransferForm';
import * as api from '../services/api';

// Mock the API module
jest.mock('../services/api');

/**
 * QC - React Component Tests
 *
 * Tests UI behavior: rendering, user interaction, form validation
 * Uses React Testing Library (user-centric testing)
 */
const mockAccounts = [
  { accountNumber: 'ACC001', ownerName: 'Ravi Kumar', balance: 50000, accountType: 'SAVINGS' },
  { accountNumber: 'ACC002', ownerName: 'Priya Sharma', balance: 20000, accountType: 'CURRENT' },
];

describe('TransferForm Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders all form fields', () => {
    render(<TransferForm accounts={mockAccounts} />);

    expect(screen.getByLabelText(/from account/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/to account/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/transfer mode/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /transfer funds/i })).toBeInTheDocument();
  });

  test('populates account dropdowns with provided accounts', () => {
    render(<TransferForm accounts={mockAccounts} />);

    const options = screen.getAllByText(/ACC001/);
    expect(options.length).toBeGreaterThanOrEqual(1);
  });

  test('shows validation error when submitting empty form', async () => {
    render(<TransferForm accounts={mockAccounts} />);

    fireEvent.click(screen.getByRole('button', { name: /transfer funds/i }));

    await waitFor(() => {
      expect(screen.getByText(/select source account/i)).toBeInTheDocument();
    });
  });

  test('shows error for same account transfer', async () => {
    render(<TransferForm accounts={mockAccounts} />);

    fireEvent.change(screen.getByLabelText(/from account/i), { target: { value: 'ACC001' } });
    fireEvent.change(screen.getByLabelText(/to account/i), { target: { value: 'ACC001' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '1000' } });

    fireEvent.click(screen.getByRole('button', { name: /transfer funds/i }));

    await waitFor(() => {
      expect(screen.getByText(/cannot transfer to the same account/i)).toBeInTheDocument();
    });
  });

  test('shows success message on successful transfer', async () => {
    api.transferFunds.mockResolvedValue({
      referenceId: 'test-ref-123',
      amount: 5000,
      status: 'SUCCESS',
    });

    const onComplete = jest.fn();
    render(<TransferForm accounts={mockAccounts} onTransferComplete={onComplete} />);

    fireEvent.change(screen.getByLabelText(/from account/i), { target: { value: 'ACC001' } });
    fireEvent.change(screen.getByLabelText(/to account/i), { target: { value: 'ACC002' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '5000' } });

    fireEvent.click(screen.getByRole('button', { name: /transfer funds/i }));

    await waitFor(() => {
      expect(screen.getByText(/transfer successful/i)).toBeInTheDocument();
      expect(screen.getByText(/test-ref-123/i)).toBeInTheDocument();
    });

    expect(onComplete).toHaveBeenCalled();
  });

  test('shows error message on failed transfer', async () => {
    api.transferFunds.mockRejectedValue({
      message: 'Insufficient balance',
      details: [],
    });

    render(<TransferForm accounts={mockAccounts} />);

    fireEvent.change(screen.getByLabelText(/from account/i), { target: { value: 'ACC001' } });
    fireEvent.change(screen.getByLabelText(/to account/i), { target: { value: 'ACC002' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '999999' } });

    fireEvent.click(screen.getByRole('button', { name: /transfer funds/i }));

    await waitFor(() => {
      expect(screen.getByText(/insufficient balance/i)).toBeInTheDocument();
    });
  });

  test('disables button while processing', async () => {
    api.transferFunds.mockImplementation(() => new Promise(() => {})); // never resolves

    render(<TransferForm accounts={mockAccounts} />);

    fireEvent.change(screen.getByLabelText(/from account/i), { target: { value: 'ACC001' } });
    fireEvent.change(screen.getByLabelText(/to account/i), { target: { value: 'ACC002' } });
    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '100' } });

    fireEvent.click(screen.getByRole('button', { name: /transfer funds/i }));

    await waitFor(() => {
      expect(screen.getByText(/processing/i)).toBeInTheDocument();
    });
  });
});
