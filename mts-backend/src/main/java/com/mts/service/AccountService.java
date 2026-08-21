package com.mts.service;

import com.mts.dto.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByUsername(String username);

    List<AccountResponse> getAllAccounts();
}
