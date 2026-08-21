package com.mts.service;

import com.mts.dto.DashboardStats;
import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.entity.Account;
import com.mts.entity.Transaction;
import com.mts.enums.TransactionStatus;
import com.mts.exception.AccountInactiveException;
import com.mts.exception.AccountNotFoundException;
import com.mts.exception.InsufficientBalanceException;
import com.mts.exception.SameAccountTransferException;
import com.mts.repository.AccountRepository;
import com.mts.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UpiTransferService implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(UpiTransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationClient notificationClient;

    public UpiTransferService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              NotificationClient notificationClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationClient = notificationClient;
    }

    @Override
    @Transactional
    public TransferResponse transferFunds(TransferRequest request) {
        log.info("Processing transfer: {} -> {}, amount: {}, mode: {}",
                request.getFromAccountNumber(), request.getToAccountNumber(),
                request.getAmount(), request.getTransferMode());

        // 1. Validate same-account transfer
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new SameAccountTransferException();
        }

        // 2. Fetch accounts
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountNumber()));

        // 3. Validate accounts are active
        if (!fromAccount.getIsActive()) {
            throw new AccountInactiveException(request.getFromAccountNumber());
        }
        if (!toAccount.getIsActive()) {
            throw new AccountInactiveException(request.getToAccountNumber());
        }

        // 4. Create transaction record (initially PENDING)
        String referenceId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .referenceId(referenceId)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .currency("INR")
                .transferMode(request.getTransferMode())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();
        transactionRepository.save(transaction);

        // 5. Check balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Insufficient balance");
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            throw new InsufficientBalanceException(
                    request.getFromAccountNumber(), fromAccount.getBalance(), request.getAmount());
        }

        // 6. Execute transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 7. Mark transaction as SUCCESS
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        log.info("Transfer successful: ref={}, amount={}", referenceId, request.getAmount());

        // 8. Build response
        TransferResponse response = toTransferResponse(transaction);

        // 9. Send notification (non-blocking — never fail the transfer)
        try {
            notificationClient.sendTransactionNotification(response);
        } catch (Exception e) {
            log.warn("Notification failed for ref={}, but transfer succeeded: {}", referenceId, e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransactionByReferenceId(String referenceId) {
        Transaction transaction = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + referenceId));
        return toTransferResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getTransactionsByAccountNumber(String accountNumber) {
        // Validate account exists
        accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .map(this::toTransferResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getTransactionsByUsername(String username) {
        return transactionRepository.findByUsername(username).stream()
                .map(this::toTransferResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::toTransferResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        List<Account> allAccounts = accountRepository.findAll();
        List<Transaction> allTransactions = transactionRepository.findAll();

        long activeAccounts = allAccounts.stream().filter(Account::getIsActive).count();

        BigDecimal totalBalance = allAccounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successCount = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS).count();
        long failedCount = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED).count();

        BigDecimal totalTransferred = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardStats.builder()
                .totalAccounts(allAccounts.size())
                .activeAccounts(activeAccounts)
                .totalBalance(totalBalance)
                .totalTransactions(allTransactions.size())
                .successfulTransactions(successCount)
                .failedTransactions(failedCount)
                .totalTransferred(totalTransferred)
                .build();
    }

    private TransferResponse toTransferResponse(Transaction transaction) {
        return TransferResponse.builder()
                .referenceId(transaction.getReferenceId())
                .fromAccountNumber(transaction.getFromAccount().getAccountNumber())
                .toAccountNumber(transaction.getToAccount().getAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transferMode(transaction.getTransferMode())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .timestamp(transaction.getCreatedAt())
                .build();
    }
}
