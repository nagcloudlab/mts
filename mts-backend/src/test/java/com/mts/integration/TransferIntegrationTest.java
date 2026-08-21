package com.mts.integration;

import tools.jackson.databind.ObjectMapper;
import com.mts.dto.TransferRequest;
import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.enums.TransferMode;
import com.mts.repository.AccountRepository;
import com.mts.repository.TransactionRepository;
import com.mts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TESTS — Transfer API -> Service -> Repository -> DB
 *
 * Tests the full transfer flow through all layers with a real H2 database.
 * Verifies that controller, service, repository and DB work together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Transfer API -> Service -> Repository -> DB")
@Tag("integration")
class TransferIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("transfer.user").email("transfer@test.com")
                .passwordHash("hash").fullName("Transfer User")
                .isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("TRF001").user(user)
                .accountType(AccountType.SAVINGS).balance(new BigDecimal("50000"))
                .currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("TRF002").user(user)
                .accountType(AccountType.CURRENT).balance(new BigDecimal("20000"))
                .currency("INR").isActive(true).build());
    }

    @Test
    @DisplayName("POST transfer -> balances updated in DB -> transaction recorded in DB")
    void transferUpdatesDBAndRecordsTransaction() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TRF001").toAccountNumber("TRF002")
                .amount(new BigDecimal("10000")).transferMode(TransferMode.UPI)
                .description("Integration test").build();

        // 1. Execute transfer via REST API
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId").isNotEmpty());

        // 2. Verify balances in DB
        Account from = accountRepository.findByAccountNumber("TRF001").orElseThrow();
        Account to = accountRepository.findByAccountNumber("TRF002").orElseThrow();
        assertEquals(0, new BigDecimal("40000").compareTo(from.getBalance()));
        assertEquals(0, new BigDecimal("30000").compareTo(to.getBalance()));

        // 3. Verify transaction recorded in DB
        var txns = transactionRepository.findByAccountNumber("TRF001");
        assertEquals(1, txns.size());
    }

    @Test
    @DisplayName("POST transfer with insufficient funds -> 400 + balances unchanged in DB")
    void insufficientFundsDoesNotAlterDB() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TRF001").toAccountNumber("TRF002")
                .amount(new BigDecimal("99999")).transferMode(TransferMode.NEFT).build();

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Balances must be unchanged
        Account from = accountRepository.findByAccountNumber("TRF001").orElseThrow();
        assertEquals(0, new BigDecimal("50000").compareTo(from.getBalance()));
    }

    @Test
    @DisplayName("GET /api/transactions/account/{number} returns transactions from DB")
    void getTransactionsByAccountReturnsFromDB() throws Exception {
        // First, create a transfer
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TRF001").toAccountNumber("TRF002")
                .amount(new BigDecimal("5000")).transferMode(TransferMode.IMPS).build();

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then, fetch history
        mockMvc.perform(get("/api/transactions/account/TRF001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromAccountNumber").value("TRF001"))
                .andExpect(jsonPath("$[0].toAccountNumber").value("TRF002"))
                .andExpect(jsonPath("$[0].amount").value(5000.0));
    }

    @Test
    @DisplayName("GET /api/transactions/stats returns correct stats from DB")
    void statsReflectTransactionsInDB() throws Exception {
        // Create a successful transfer
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("TRF001").toAccountNumber("TRF002")
                .amount(new BigDecimal("1000")).transferMode(TransferMode.UPI).build();

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Check stats
        mockMvc.perform(get("/api/transactions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(1))
                .andExpect(jsonPath("$.successfulTransactions").value(1))
                .andExpect(jsonPath("$.totalTransferred").value(1000.0));
    }
}
