package com.mts.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String accountNumber, BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient balance in account %s. Available: %s, Requested: %s",
                accountNumber, available, requested));
    }
}
