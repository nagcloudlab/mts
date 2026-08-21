package com.mts.repository;

import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QC - Repository Tests using @DataJpaTest
 *
 * Uses H2 in-memory database
 * Tests custom queries and JPA mappings
 */
@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .username("repo.test")
                .email("repo@test.com")
                .passwordHash("hash")
                .fullName("Repo Test")
                .isActive(true)
                .build();
        savedUser = userRepository.save(savedUser);

        Account acc1 = Account.builder()
                .accountNumber("REPO001").user(savedUser)
                .accountType(AccountType.SAVINGS).balance(new BigDecimal("10000"))
                .currency("INR").isActive(true).build();

        Account acc2 = Account.builder()
                .accountNumber("REPO002").user(savedUser)
                .accountType(AccountType.CURRENT).balance(new BigDecimal("25000"))
                .currency("INR").isActive(true).build();

        Account inactiveAcc = Account.builder()
                .accountNumber("REPO003").user(savedUser)
                .accountType(AccountType.SAVINGS).balance(new BigDecimal("5000"))
                .currency("INR").isActive(false).build();

        accountRepository.saveAll(List.of(acc1, acc2, inactiveAcc));
    }

    @Test
    @DisplayName("Should find account by account number")
    void testFindByAccountNumber() {
        Optional<Account> result = accountRepository.findByAccountNumber("REPO001");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.get().getBalance()));
    }

    @Test
    @DisplayName("Should return empty for non-existent account")
    void testFindByAccountNumberNotFound() {
        Optional<Account> result = accountRepository.findByAccountNumber("NONEXISTENT");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find all accounts by user ID")
    void testFindByUserId() {
        List<Account> accounts = accountRepository.findByUserId(savedUser.getId());
        assertEquals(3, accounts.size());
    }

    @Test
    @DisplayName("Should find only active accounts by username")
    void testFindActiveAccountsByUsername() {
        List<Account> activeAccounts = accountRepository.findActiveAccountsByUsername("repo.test");
        assertEquals(2, activeAccounts.size());
        assertTrue(activeAccounts.stream().allMatch(Account::getIsActive));
    }

    @Test
    @DisplayName("Should check account existence")
    void testExistsByAccountNumber() {
        assertTrue(accountRepository.existsByAccountNumber("REPO001"));
        assertFalse(accountRepository.existsByAccountNumber("FAKE"));
    }
}
