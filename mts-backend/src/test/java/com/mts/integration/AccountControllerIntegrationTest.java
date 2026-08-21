package com.mts.integration;

import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.repository.AccountRepository;
import com.mts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TESTS — AccountController + AccountService + AccountRepository + H2 DB
 *
 * MTS equivalent of "Order API saves to database correctly"
 * Full Spring context, real HTTP layer, real database (H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Account API -> Service -> Repository -> DB")
@Tag("integration")
class AccountControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.builder()
                .username("intg.user").email("intg@test.com")
                .passwordHash("hash").fullName("Integration User")
                .isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("INTG001").user(savedUser)
                .accountType(AccountType.SAVINGS).balance(new BigDecimal("50000"))
                .currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("INTG002").user(savedUser)
                .accountType(AccountType.CURRENT).balance(new BigDecimal("120000"))
                .currency("INR").isActive(true).build());
    }

    @Test
    @DisplayName("GET /api/accounts returns all accounts from DB")
    void getAllAccountsReturnsFromDB() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.accountNumber == 'INTG001')].ownerName",
                        hasItem("Integration User")));
    }

    @Test
    @DisplayName("GET /api/accounts/{number} returns correct account from DB")
    void getAccountByNumberReturnsFromDB() throws Exception {
        mockMvc.perform(get("/api/accounts/INTG001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("INTG001"))
                .andExpect(jsonPath("$.ownerName").value("Integration User"))
                .andExpect(jsonPath("$.balance").value(50000.0))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    @DisplayName("GET /api/accounts/{number} returns 404 for non-existent account")
    void getAccountNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/accounts/NONEXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account Not Found"));
    }

    @Test
    @DisplayName("GET /api/accounts/user/{username} returns accounts for user")
    void getAccountsByUsernameReturnsFromDB() throws Exception {
        mockMvc.perform(get("/api/accounts/user/intg.user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].accountNumber",
                        containsInAnyOrder("INTG001", "INTG002")));
    }
}
