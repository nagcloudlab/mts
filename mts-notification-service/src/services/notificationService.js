// In-memory audit log (in production, this would be a database or message queue)
const auditLog = [];

function logTransaction(transaction) {
  const entry = {
    id: auditLog.length + 1,
    referenceId: transaction.referenceId,
    fromAccount: transaction.fromAccountNumber,
    toAccount: transaction.toAccountNumber,
    amount: transaction.amount,
    status: transaction.status,
    transferMode: transaction.transferMode,
    notifiedAt: new Date().toISOString(),
    channel: 'SMS', // simulated
  };

  auditLog.push(entry);
  console.log(`[NOTIFICATION] Transaction ${entry.referenceId} | ${entry.fromAccount} -> ${entry.toAccount} | INR ${entry.amount} | ${entry.status}`);

  return entry;
}

function getAuditLog() {
  return [...auditLog].reverse(); // newest first
}

function getAuditByReference(referenceId) {
  return auditLog.find((entry) => entry.referenceId === referenceId) || null;
}

function getStats() {
  const total = auditLog.length;
  const successful = auditLog.filter((e) => e.status === 'SUCCESS').length;
  const failed = auditLog.filter((e) => e.status === 'FAILED').length;

  return { total, successful, failed };
}

module.exports = { logTransaction, getAuditLog, getAuditByReference, getStats };
