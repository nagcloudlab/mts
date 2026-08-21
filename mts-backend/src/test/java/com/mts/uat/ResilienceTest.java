package com.mts.uat;

import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.entity.Account;
import com.mts.entity.Transaction;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.enums.TransactionStatus;
import com.mts.enums.TransferMode;
import com.mts.exception.InsufficientBalanceException;
import com.mts.repository.AccountRepository;
import com.mts.repository.TransactionRepository;
import com.mts.repository.UserRepository;
import com.mts.service.NotificationClient;
import com.mts.service.TransferService;
import com.mts.service.UpiTransferService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  UAT: Resilience & Data Integrity Tests (Non-Functional)     ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Run before release — NOT every commit                       ║
 * ║  Validates: Failure recovery, money conservation,            ║
 * ║             notification failure handling, data consistency   ║
 * ╚══════════════════════════════════════════════════════════════╝
 */

@Tag("resilience")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UAT: Resilience & Data Integrity Tests")
class ResilienceTest {

    @Autowired private TransferService transferService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    private BigDecimal initialTotalBalance;

    @BeforeEach
    void setUp() {
        User user1 = userRepository.save(User.builder()
                .username("res.user1").email("res1@test.com")
                .passwordHash("hash").fullName("User One").isActive(true).build());

        User user2 = userRepository.save(User.builder()
                .username("res.user2").email("res2@test.com")
                .passwordHash("hash").fullName("User Two").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("RES001").user(user1).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("50000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("RES002").user(user1).accountType(AccountType.CURRENT)
                .balance(new BigDecimal("80000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("RES003").user(user2).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("30000")).currency("INR").isActive(true).build());

        initialTotalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Notification Service Failure ─────────────────────────────

    @Nested
    @DisplayName("Notification Service Down")
    class NotificationDown {

        @Test
        @DisplayName("Transfer succeeds even when notification service throws")
        void transferSucceedsWhenNotificationFails() {
            NotificationClient failingClient = mock(NotificationClient.class);
            doThrow(new RuntimeException("Connection refused"))
                    .when(failingClient).sendTransactionNotification(any());

            TransferService service = new UpiTransferService(
                    accountRepository, transactionRepository, failingClient);

            TransferResponse response = service.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES001").toAccountNumber("RES003")
                    .amount(new BigDecimal("5000")).transferMode(TransferMode.UPI).build());

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        }
    }

    // ── Graceful Error Recovery ──────────────────────────────────

    @Nested
    @DisplayName("Graceful Recovery")
    class GracefulRecovery {

        @Test
        @DisplayName("System recovers after failed transfer")
        void recoveryAfterFailure() {
            assertThrows(InsufficientBalanceException.class, () ->
                    transferService.transferFunds(TransferRequest.builder()
                            .fromAccountNumber("RES001").toAccountNumber("RES003")
                            .amount(new BigDecimal("99999")).transferMode(TransferMode.UPI).build()));

            TransferResponse response = transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES001").toAccountNumber("RES003")
                    .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build());

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        }

        @Test
        @DisplayName("Multiple consecutive failures do not corrupt state")
        void multipleFailuresDontCorrupt() {
            BigDecimal before = accountRepository.findByAccountNumber("RES001").orElseThrow().getBalance();

            for (int i = 0; i < 5; i++) {
                assertThrows(InsufficientBalanceException.class, () ->
                        transferService.transferFunds(TransferRequest.builder()
                                .fromAccountNumber("RES001").toAccountNumber("RES003")
                                .amount(new BigDecimal("99999")).transferMode(TransferMode.UPI).build()));
            }

            BigDecimal after = accountRepository.findByAccountNumber("RES001").orElseThrow().getBalance();
            assertEquals(0, before.compareTo(after), "Balance unchanged after failures");
        }
    }

    // ── Money Conservation ───────────────────────────────────────

    @Nested
    @DisplayName("Money Conservation")
    class MoneyConservation {

        @Test
        @DisplayName("Total balance conserved after successful transfer")
        void conservedAfterSuccess() {
            transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES001").toAccountNumber("RES003")
                    .amount(new BigDecimal("15000")).transferMode(TransferMode.UPI).build());

            BigDecimal totalAfter = accountRepository.findAll().stream()
                    .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, initialTotalBalance.compareTo(totalAfter),
                    "No money created or destroyed");
        }

        @Test
        @DisplayName("Total balance conserved after failed transfer")
        void conservedAfterFailure() {
            assertThrows(InsufficientBalanceException.class, () ->
                    transferService.transferFunds(TransferRequest.builder()
                            .fromAccountNumber("RES001").toAccountNumber("RES003")
                            .amount(new BigDecimal("99999")).transferMode(TransferMode.UPI).build()));

            BigDecimal totalAfter = accountRepository.findAll().stream()
                    .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, initialTotalBalance.compareTo(totalAfter));
        }

        @Test
        @DisplayName("Total balance conserved after circular transfers")
        void conservedAfterCircularTransfers() {
            transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES001").toAccountNumber("RES003")
                    .amount(new BigDecimal("10000")).transferMode(TransferMode.UPI).build());
            transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES003").toAccountNumber("RES002")
                    .amount(new BigDecimal("5000")).transferMode(TransferMode.NEFT).build());
            transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES002").toAccountNumber("RES001")
                    .amount(new BigDecimal("3000")).transferMode(TransferMode.IMPS).build());

            BigDecimal totalAfter = accountRepository.findAll().stream()
                    .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, initialTotalBalance.compareTo(totalAfter));
        }
    }

    // ── Transaction Record Integrity ─────────────────────────────

    @Nested
    @DisplayName("Transaction Record Integrity")
    class RecordIntegrity {

        @Test
        @DisplayName("Successful transfer has a SUCCESS record")
        void successRecord() {
            TransferResponse response = transferService.transferFunds(TransferRequest.builder()
                    .fromAccountNumber("RES001").toAccountNumber("RES003")
                    .amount(new BigDecimal("5000")).transferMode(TransferMode.UPI).build());

            TransferResponse found = transferService.getTransactionByReferenceId(response.getReferenceId());
            assertEquals(TransactionStatus.SUCCESS, found.getStatus());
        }

        @Test
        @DisplayName("Failed transfer has a FAILED record with reason")
        void failedRecord() {
            assertThrows(InsufficientBalanceException.class, () ->
                    transferService.transferFunds(TransferRequest.builder()
                            .fromAccountNumber("RES001").toAccountNumber("RES003")
                            .amount(new BigDecimal("99999")).transferMode(TransferMode.UPI).build()));

            List<Transaction> txns = transactionRepository.findByAccountNumber("RES001");
            Transaction failed = txns.stream()
                    .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                    .findFirst().orElse(null);

            assertNotNull(failed, "Failed transaction should be recorded");
            assertTrue(failed.getFailureReason().contains("Insufficient"));
        }
    }
}
