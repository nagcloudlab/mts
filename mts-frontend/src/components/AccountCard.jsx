function AccountCard({ account }) {
  return (
    <div className="account-card">
      <div className="account-number">{account.accountNumber}</div>
      <div className="account-owner">{account.ownerName}</div>
      <span className="account-type">{account.accountType}</span>
      <div className="account-balance">
        <span className="currency">INR </span>
        {Number(account.balance).toLocaleString('en-IN', {
          minimumFractionDigits: 2,
        })}
      </div>
    </div>
  );
}

export default AccountCard;
