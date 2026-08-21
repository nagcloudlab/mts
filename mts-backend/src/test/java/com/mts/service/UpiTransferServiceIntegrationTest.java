package com.mts.service;

import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.enums.TransactionStatus;
import com.mts.enums.TransferMode;
import com.mts.exception.InsufficientBalanceException;
import com.mts.repository.AccountRepository;
import com.mts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA - Integration Tests for UpiTransferService
 *
 * Uses H2 in-memory database (test profile)
 * Tests real Spring context with actual DB operations
 * Each test is @Transactional so it rolls back after completion
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("integration")
class UpiTransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        // Create test user
        User user = User.builder()
                .username("test.user")
                .email("test@example.com")
                .passwordHash("$2a$10$test")
                .fullName("Test User")
                .phone("9999999999")
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // Create test accounts
        fromAccount = Account.builder()
                .accountNumber("TEST001")
                .user(user)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("50000.00"))
                .currency("INR")
                .isActive(true)
                .build();

        toAccount = Account.builder()
                .accountNumber("TEST002")
                .user(user)
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("20000.00"))
                .currency("INR")
                .isActive(true)
                .build();

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    @Test
    @DisplayName("Integration: Full transfer flow with real database")
    void testFullTransferFlow() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TEST001")
                .toAccountNumber("TEST002")
                .amount(new BigDecimal("10000.00"))
                .transferMode(TransferMode.UPI)
                .description("Integration test transfer")
                .build();

        // Act
        TransferResponse response = transferService.transferFunds(request);

        // Assert - Response
        assertNotNull(response.getReferenceId());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("10000.00"), response.getAmount());

        // Assert - Database state
        Account updatedFrom = accountRepository.findByAccountNumber("TEST001").orElseThrow();
        Account updatedTo = accountRepository.findByAccountNumber("TEST002").orElseThrow();

        assertEquals(new BigDecimal("40000.00"), updatedFrom.getBalance());
        assertEquals(new BigDecimal("30000.00"), updatedTo.getBalance());
    }

    @Test
    @DisplayName("Integration: Multiple transfers reduce balance correctly")
    void testMultipleTransfers() {
        // Transfer 1
        TransferRequest req1 = TransferRequest.builder()
                .fromAccountNumber("TEST001").toAccountNumber("TEST002")
                .amount(new BigDecimal("10000.00")).transferMode(TransferMode.NEFT).build();
        transferService.transferFunds(req1);

        // Transfer 2
        TransferRequest req2 = TransferRequest.builder()
                .fromAccountNumber("TEST001").toAccountNumber("TEST002")
                .amount(new BigDecimal("15000.00")).transferMode(TransferMode.IMPS).build();
        transferService.transferFunds(req2);

        // Assert cumulative
        Account updatedFrom = accountRepository.findByAccountNumber("TEST001").orElseThrow();
        assertEquals(new BigDecimal("25000.00"), updatedFrom.getBalance());
    }

    @Test
    @DisplayName("Integration: Insufficient balance does not alter balances")
    void testInsufficientBalanceNoSideEffects() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TEST001").toAccountNumber("TEST002")
                .amount(new BigDecimal("99999.00")).transferMode(TransferMode.UPI).build();

        assertThrows(InsufficientBalanceException.class, () ->
                transferService.transferFunds(request));

        // Balances should be unchanged
        Account unchangedFrom = accountRepository.findByAccountNumber("TEST001").orElseThrow();
        assertEquals(new BigDecimal("50000.00"), unchangedFrom.getBalance());
    }

    @Test
    @DisplayName("Integration: Transaction history is recorded")
    void testTransactionHistoryRecorded() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TEST001").toAccountNumber("TEST002")
                .amount(new BigDecimal("5000.00")).transferMode(TransferMode.RTGS)
                .description("History test").build();

        TransferResponse response = transferService.transferFunds(request);

        // Fetch by reference
        TransferResponse found = transferService.getTransactionByReferenceId(response.getReferenceId());
        assertNotNull(found);
        assertEquals("TEST001", found.getFromAccountNumber());
        assertEquals("TEST002", found.getToAccountNumber());
        assertEquals(TransactionStatus.SUCCESS, found.getStatus());
    }
}
