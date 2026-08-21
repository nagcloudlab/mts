package com.mts.service;

import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.entity.Account;
import com.mts.entity.Transaction;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.enums.TransactionStatus;
import com.mts.enums.TransferMode;
import com.mts.exception.AccountInactiveException;
import com.mts.exception.AccountNotFoundException;
import com.mts.exception.InsufficientBalanceException;
import com.mts.exception.SameAccountTransferException;
import com.mts.repository.AccountRepository;
import com.mts.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QC - Unit Tests for UpiTransferService
 *
 * Pattern: Arrange -> Act -> Assert (AAA)
 * Isolation: All dependencies are mocked using Mockito
 * Coverage: Happy path + edge cases + error scenarios
 */
@Tag("unit")
class UpiTransferServiceUnitTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private NotificationClient notificationClient;
    private UpiTransferService transferService;

    // Test fixtures
    private User testUser;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        notificationClient = mock(NotificationClient.class);
        transferService = new UpiTransferService(accountRepository, transactionRepository, notificationClient);

        // Common test data
        testUser = User.builder().id(1L).username("ravi.kumar").fullName("Ravi Kumar").build();
        fromAccount = Account.builder()
                .id(1L).accountNumber("ACC001").user(testUser)
                .accountType(AccountType.SAVINGS).balance(new BigDecimal("50000.00"))
                .currency("INR").isActive(true).build();
        toAccount = Account.builder()
                .id(2L).accountNumber("ACC002").user(testUser)
                .accountType(AccountType.CURRENT).balance(new BigDecimal("20000.00"))
                .currency("INR").isActive(true).build();
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should transfer funds successfully between two accounts")
        void testSuccessfulTransfer() {
            // Arrange
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001")
                    .toAccountNumber("ACC002")
                    .amount(new BigDecimal("5000.00"))
                    .transferMode(TransferMode.UPI)
                    .description("Test transfer")
                    .build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TransferResponse response = transferService.transferFunds(request);

            // Assert
            assertNotNull(response);
            assertNotNull(response.getReferenceId());
            assertEquals("ACC001", response.getFromAccountNumber());
            assertEquals("ACC002", response.getToAccountNumber());
            assertEquals(new BigDecimal("5000.00"), response.getAmount());
            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals(TransferMode.UPI, response.getTransferMode());

            // Verify balances were updated
            assertEquals(new BigDecimal("45000.00"), fromAccount.getBalance());
            assertEquals(new BigDecimal("25000.00"), toAccount.getBalance());

            // Verify repository interactions
            verify(accountRepository).save(fromAccount);
            verify(accountRepository).save(toAccount);
            verify(transactionRepository, times(2)).save(any(Transaction.class)); // PENDING + SUCCESS
            verify(notificationClient).sendTransactionNotification(any(TransferResponse.class));
        }

        @Test
        @DisplayName("Should debit exact amount from sender")
        void testExactDebitAmount() {
            // Arrange
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("12345.67")).transferMode(TransferMode.NEFT).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            transferService.transferFunds(request);

            // Assert
            assertEquals(new BigDecimal("37654.33"), fromAccount.getBalance());
            assertEquals(new BigDecimal("32345.67"), toAccount.getBalance());
        }
    }

    @Nested
    @DisplayName("Validation & Error Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception for same account transfer")
        void testSameAccountTransfer() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC001")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            assertThrows(SameAccountTransferException.class, () ->
                    transferService.transferFunds(request));

            // Verify no DB calls were made
            verify(accountRepository, never()).findByAccountNumber(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when from-account not found")
        void testFromAccountNotFound() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("INVALID").toAccountNumber("ACC002")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            AccountNotFoundException ex = assertThrows(AccountNotFoundException.class, () ->
                    transferService.transferFunds(request));

            assertTrue(ex.getMessage().contains("INVALID"));
        }

        @Test
        @DisplayName("Should throw exception when to-account not found")
        void testToAccountNotFound() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("INVALID")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () ->
                    transferService.transferFunds(request));
        }

        @Test
        @DisplayName("Should throw exception for insufficient balance")
        void testInsufficientBalance() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("999999.00")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertThrows(InsufficientBalanceException.class, () ->
                    transferService.transferFunds(request));

            // Verify transaction was saved as FAILED
            ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, atLeast(1)).save(txnCaptor.capture());
            Transaction failedTxn = txnCaptor.getAllValues().stream()
                    .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                    .findFirst().orElse(null);
            assertNotNull(failedTxn);
            assertEquals("Insufficient balance", failedTxn.getFailureReason());
        }

        @Test
        @DisplayName("Should throw exception when from-account is inactive")
        void testInactiveFromAccount() {
            fromAccount.setIsActive(false);
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));

            assertThrows(AccountInactiveException.class, () ->
                    transferService.transferFunds(request));
        }

        @Test
        @DisplayName("Should throw exception when to-account is inactive")
        void testInactiveToAccount() {
            toAccount.setIsActive(false);
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));

            assertThrows(AccountInactiveException.class, () ->
                    transferService.transferFunds(request));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle transfer of entire balance")
        void testTransferEntireBalance() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("50000.00")).transferMode(TransferMode.IMPS).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransferResponse response = transferService.transferFunds(request);

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals(BigDecimal.ZERO.setScale(2), fromAccount.getBalance());
            assertEquals(new BigDecimal("70000.00"), toAccount.getBalance());
        }

        @Test
        @DisplayName("Should still succeed even if notification fails")
        void testTransferSucceedsWhenNotificationFails() {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(toAccount));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("Notification service down"))
                    .when(notificationClient).sendTransactionNotification(any());

            // Should NOT throw - notification failure is non-blocking
            TransferResponse response = transferService.transferFunds(request);
            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        }
    }
}
