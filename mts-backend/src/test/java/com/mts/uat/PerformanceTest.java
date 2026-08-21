package com.mts.uat;

import tools.jackson.databind.ObjectMapper;
import com.mts.dto.TransferRequest;
import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
import com.mts.enums.TransferMode;
import com.mts.repository.AccountRepository;
import com.mts.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  UAT: Performance Tests (Non-Functional)                     ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Run before release — NOT every commit                       ║
 * ║  Validates: API response times within acceptable thresholds  ║
 * ║  Read APIs < 500ms | Write APIs < 1000ms                     ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Tag("performance")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UAT: Performance Tests")
class PerformanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("perf.user").email("perf@test.com")
                .passwordHash("hash").fullName("Perf User").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("PERF001").user(user).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("PERF002").user(user).accountType(AccountType.CURRENT)
                .balance(new BigDecimal("500000")).currency("INR").isActive(true).build());
    }

    @Test
    @DisplayName("GET /api/accounts responds within 500ms")
    void accountListPerformance() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/api/accounts")).andExpect(status().isOk());
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 500, "Account list should respond within 500ms, took: " + duration + "ms");
    }

    @Test
    @DisplayName("GET /api/accounts/{number} responds within 500ms")
    void singleAccountPerformance() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/api/accounts/PERF001")).andExpect(status().isOk());
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 500, "Single account should respond within 500ms, took: " + duration + "ms");
    }

    @Test
    @DisplayName("GET /api/transactions/stats responds within 500ms")
    void statsPerformance() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/api/transactions/stats")).andExpect(status().isOk());
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 500, "Stats API should respond within 500ms, took: " + duration + "ms");
    }

    @Test
    @DisplayName("POST transfer responds within 1000ms")
    void transferPerformance() throws Exception {
        TransferRequest req = TransferRequest.builder()
                .fromAccountNumber("PERF001").toAccountNumber("PERF002")
                .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

        long start = System.currentTimeMillis();
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 1000, "Transfer should respond within 1000ms, took: " + duration + "ms");
    }

    @Test
    @DisplayName("10 sequential transfers complete within 5 seconds")
    void throughputTest() throws Exception {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            TransferRequest req = TransferRequest.builder()
                    .fromAccountNumber("PERF001").toAccountNumber("PERF002")
                    .amount(new BigDecimal("10")).transferMode(TransferMode.UPI).build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 5000, "10 transfers should complete within 5s, took: " + duration + "ms");
    }
}
