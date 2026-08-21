package com.mts.unit;

import com.mts.dto.AccountResponse;
import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.exception.AccountNotFoundException;
import com.mts.repository.AccountRepository;
import com.mts.service.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS — AccountServiceImpl
 *
 * MTS equivalent of "calculateTotal() returns correct sum"
 * Each method tested in isolation with mocked dependencies.
 */
@DisplayName("Unit: AccountServiceImpl")
@Tag("unit")
class AccountServiceUnitTest {

    private AccountRepository accountRepository;
    private AccountServiceImpl accountService;
    private User testUser;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountService = new AccountServiceImpl(accountRepository);

        testUser = User.builder()
                .id(1L).username("ravi.kumar").fullName("Ravi Kumar")
                .email("ravi@test.com").passwordHash("hash").build();
    }

    private Account buildAccount(String number, BigDecimal balance, AccountType type) {
        return Account.builder()
                .id(1L).accountNumber(number).user(testUser)
                .accountType(type).balance(balance)
                .currency("INR").isActive(true).build();
    }

    @Nested
    @DisplayName("getAccountByNumber()")
    class GetAccountByNumber {

        @Test
        @DisplayName("Should return account response for valid account number")
        void returnsAccountForValidNumber() {
            Account account = buildAccount("ACC001", new BigDecimal("50000"), AccountType.SAVINGS);
            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));

            AccountResponse response = accountService.getAccountByNumber("ACC001");

            assertEquals("ACC001", response.getAccountNumber());
            assertEquals("Ravi Kumar", response.getOwnerName());
            assertEquals(new BigDecimal("50000"), response.getBalance());
            assertEquals(AccountType.SAVINGS, response.getAccountType());
            assertEquals("INR", response.getCurrency());
            assertTrue(response.getIsActive());
        }

        @Test
        @DisplayName("Should throw AccountNotFoundException for invalid number")
        void throwsForInvalidNumber() {
            when(accountRepository.findByAccountNumber("FAKE")).thenReturn(Optional.empty());

            AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                    () -> accountService.getAccountByNumber("FAKE"));
            assertTrue(ex.getMessage().contains("FAKE"));
        }
    }

    @Nested
    @DisplayName("getAllAccounts()")
    class GetAllAccounts {

        @Test
        @DisplayName("Should return all accounts mapped to DTOs")
        void returnsAllAccountsAsDTOs() {
            List<Account> accounts = List.of(
                    buildAccount("ACC001", new BigDecimal("50000"), AccountType.SAVINGS),
                    buildAccount("ACC002", new BigDecimal("120000"), AccountType.CURRENT)
            );
            when(accountRepository.findAll()).thenReturn(accounts);

            List<AccountResponse> result = accountService.getAllAccounts();

            assertEquals(2, result.size());
            assertEquals("ACC001", result.get(0).getAccountNumber());
            assertEquals("ACC002", result.get(1).getAccountNumber());
        }

        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void returnsEmptyListWhenNoAccounts() {
            when(accountRepository.findAll()).thenReturn(List.of());

            List<AccountResponse> result = accountService.getAllAccounts();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAccountsByUsername()")
    class GetAccountsByUsername {

        @Test
        @DisplayName("Should return only active accounts for username")
        void returnsActiveAccountsForUser() {
            List<Account> accounts = List.of(
                    buildAccount("ACC001", new BigDecimal("50000"), AccountType.SAVINGS)
            );
            when(accountRepository.findActiveAccountsByUsername("ravi.kumar")).thenReturn(accounts);

            List<AccountResponse> result = accountService.getAccountsByUsername("ravi.kumar");

            assertEquals(1, result.size());
            assertTrue(result.get(0).getIsActive());
        }

        @Test
        @DisplayName("Should return empty list for unknown username")
        void returnsEmptyForUnknownUser() {
            when(accountRepository.findActiveAccountsByUsername("unknown")).thenReturn(List.of());

            List<AccountResponse> result = accountService.getAccountsByUsername("unknown");

            assertTrue(result.isEmpty());
        }
    }
}
