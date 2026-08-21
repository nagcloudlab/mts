package com.mts.uat;

import com.mts.entity.Account;
import com.mts.entity.User;
import com.mts.enums.AccountType;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  UAT: Security Tests (Non-Functional)                       ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Run before release — NOT every commit                       ║
 * ║  Validates: SQL injection, XSS, malformed input, HTTP 405    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Tag("security")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UAT: Security Tests")
class SecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("sec.user").email("sec@test.com")
                .passwordHash("hash").fullName("Security User").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("SEC001").user(user).accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("50000")).currency("INR").isActive(true).build());

        accountRepository.save(Account.builder()
                .accountNumber("SEC002").user(user).accountType(AccountType.CURRENT)
                .balance(new BigDecimal("30000")).currency("INR").isActive(true).build());
    }

    // ── SQL Injection ────────────────────────────────────────────

    @Test
    @DisplayName("SQL injection in path variable returns 404, not 500")
    void sqlInjectionInPath() throws Exception {
        mockMvc.perform(get("/api/accounts/' OR '1'='1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SQL injection in transfer body is rejected safely")
    void sqlInjectionInBody() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "fromAccountNumber": "'; DROP TABLE accounts;--",
                                "toAccountNumber": "SEC002",
                                "amount": 100,
                                "transferMode": "UPI"
                            }
                        """))
                .andExpect(status().isNotFound());
    }

    // ── XSS Prevention ──────────────────────────────────────────

    @Test
    @DisplayName("Script tag in description is stored as plain text")
    void xssInDescription() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "fromAccountNumber": "SEC001",
                                "toAccountNumber": "SEC002",
                                "amount": 100,
                                "transferMode": "UPI",
                                "description": "<script>alert('xss')</script>"
                            }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("<script>alert('xss')</script>"));
    }

    // ── Malformed Input ─────────────────────────────────────────

    @Test
    @DisplayName("Invalid JSON returns 400")
    void invalidJson() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json!!!}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Empty body returns 400")
    void emptyBody() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wrong content type returns 415")
    void wrongContentType() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("some text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Negative amount is rejected")
    void negativeAmount() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "fromAccountNumber": "SEC001",
                                "toAccountNumber": "SEC002",
                                "amount": -5000,
                                "transferMode": "UPI"
                            }
                        """))
                .andExpect(status().isBadRequest());
    }

    // ── HTTP Method Security ────────────────────────────────────

    @Test
    @DisplayName("PUT on transfer endpoint returns 405")
    void putReturns405() throws Exception {
        mockMvc.perform(put("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("DELETE on accounts returns 405")
    void deleteReturns405() throws Exception {
        mockMvc.perform(delete("/api/accounts/SEC001"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ── Error Response Safety ───────────────────────────────────

    @Test
    @DisplayName("Error responses do not leak stack traces or package names")
    void noStackTraceLeaks() throws Exception {
        String body = mockMvc.perform(get("/api/accounts/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("com.mts"), "Should not contain package names");
        assertFalse(body.contains(".java"), "Should not contain file names");
    }
}
