package com.mts.service;

import com.mts.dto.DashboardStats;
import com.mts.dto.TransferRequest;
import com.mts.dto.TransferResponse;

import java.util.List;

public interface TransferService {

    TransferResponse transferFunds(TransferRequest request);

    TransferResponse getTransactionByReferenceId(String referenceId);

    List<TransferResponse> getTransactionsByAccountNumber(String accountNumber);

    List<TransferResponse> getTransactionsByUsername(String username);

    List<TransferResponse> getAllTransactions();

    DashboardStats getDashboardStats();
}
