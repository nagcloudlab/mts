package com.mts.controller;

import com.mts.dto.DashboardStats;
import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;
import com.mts.service.TransferService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    public ResponseEntity<List<TransferResponse>> getAllTransactions() {
        log.info("GET /api/transactions - Fetching all transactions");
        return ResponseEntity.ok(transferService.getAllTransactions());
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        log.info("GET /api/transactions/stats");
        return ResponseEntity.ok(transferService.getDashboardStats());
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("POST /api/transactions/transfer - {} -> {}, amount: {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());
        TransferResponse response = transferService.transferFunds(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{referenceId}")
    public ResponseEntity<TransferResponse> getTransaction(@PathVariable String referenceId) {
        log.info("GET /api/transactions/{}", referenceId);
        return ResponseEntity.ok(transferService.getTransactionByReferenceId(referenceId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransferResponse>> getByAccount(@PathVariable String accountNumber) {
        log.info("GET /api/transactions/account/{}", accountNumber);
        return ResponseEntity.ok(transferService.getTransactionsByAccountNumber(accountNumber));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<TransferResponse>> getByUser(@PathVariable String username) {
        log.info("GET /api/transactions/user/{}", username);
        return ResponseEntity.ok(transferService.getTransactionsByUsername(username));
    }
}
