package com.mts.controller;

import tools.jackson.databind.ObjectMapper;
import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.enums.TransactionStatus;
import com.mts.enums.TransferMode;
import com.mts.exception.AccountNotFoundException;
import com.mts.exception.InsufficientBalanceException;
import com.mts.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QC - Controller Layer Tests using MockMvc
 *
 * Tests HTTP layer in isolation (no real service/DB)
 * Validates: request validation, status codes, JSON response structure
 */
@WebMvcTest(TransactionController.class)
@Tag("integration")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/transactions/transfer - 201 Created on success")
    void testTransferSuccess() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                .amount(new BigDecimal("5000.00")).transferMode(TransferMode.UPI)
                .description("Test").build();

        TransferResponse response = TransferResponse.builder()
                .referenceId("ref-123").fromAccountNumber("ACC001").toAccountNumber("ACC002")
                .amount(new BigDecimal("5000.00")).currency("INR")
                .transferMode(TransferMode.UPI).status(TransactionStatus.SUCCESS)
                .timestamp(LocalDateTime.now()).build();

        when(transferService.transferFunds(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceId").value("ref-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(5000.00));
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - 400 on missing fields")
    void testTransferValidationError() throws Exception {
        // Empty request - should fail validation
        String emptyRequest = "{}";

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - 400 on amount below minimum")
    void testTransferAmountTooLow() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                .amount(new BigDecimal("0.50")).transferMode(TransferMode.UPI).build();

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - 404 on account not found")
    void testTransferAccountNotFound() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("INVALID").toAccountNumber("ACC002")
                .amount(new BigDecimal("100")).transferMode(TransferMode.UPI).build();

        when(transferService.transferFunds(any()))
                .thenThrow(new AccountNotFoundException("INVALID"));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account Not Found"));
    }

    @Test
    @DisplayName("POST /api/transactions/transfer - 400 on insufficient balance")
    void testTransferInsufficientBalance() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC001").toAccountNumber("ACC002")
                .amount(new BigDecimal("999999")).transferMode(TransferMode.UPI).build();

        when(transferService.transferFunds(any()))
                .thenThrow(new InsufficientBalanceException("ACC001",
                        new BigDecimal("50000"), new BigDecimal("999999")));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"));
    }

    @Test
    @DisplayName("GET /api/transactions/{referenceId} - 200 on found")
    void testGetTransactionByRef() throws Exception {
        TransferResponse response = TransferResponse.builder()
                .referenceId("ref-456").fromAccountNumber("ACC001").toAccountNumber("ACC002")
                .amount(new BigDecimal("1000")).status(TransactionStatus.SUCCESS).build();

        when(transferService.getTransactionByReferenceId("ref-456")).thenReturn(response);

        mockMvc.perform(get("/api/transactions/ref-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("ref-456"));
    }
}
