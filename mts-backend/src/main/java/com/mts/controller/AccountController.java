package com.mts.controller;

import com.mts.dto.AccountResponse;
import com.mts.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("GET /api/accounts - Fetching all accounts");
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        log.info("GET /api/accounts/{} - Fetching account", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable String username) {
        log.info("GET /api/accounts/user/{} - Fetching accounts for user", username);
        return ResponseEntity.ok(accountService.getAccountsByUsername(username));
    }
}
