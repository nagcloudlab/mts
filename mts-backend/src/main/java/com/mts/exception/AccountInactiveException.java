package com.mts.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(String accountNumber) {
        super("Account is inactive: " + accountNumber);
    }
}
