package com.mts.unit;

import com.mts.exception.AccountInactiveException;
import com.mts.exception.AccountNotFoundException;
import com.mts.exception.InsufficientBalanceException;
import com.mts.exception.SameAccountTransferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TESTS — Custom Exception classes
 *
 * Ensures exception messages are formatted correctly.
 */
@DisplayName("Unit: Custom Exceptions")
@Tag("unit")
class ExceptionUnitTest {

    @Test
    @DisplayName("AccountNotFoundException contains account number in message")
    void accountNotFoundMessage() {
        AccountNotFoundException ex = new AccountNotFoundException("ACC999");
        assertTrue(ex.getMessage().contains("ACC999"));
    }

    @Test
    @DisplayName("InsufficientBalanceException contains all amount details")
    void insufficientBalanceMessage() {
        InsufficientBalanceException ex = new InsufficientBalanceException(
                "ACC001", new BigDecimal("5000"), new BigDecimal("10000"));
        assertTrue(ex.getMessage().contains("ACC001"));
        assertTrue(ex.getMessage().contains("5000"));
        assertTrue(ex.getMessage().contains("10000"));
    }

    @Test
    @DisplayName("SameAccountTransferException has correct message")
    void sameAccountMessage() {
        SameAccountTransferException ex = new SameAccountTransferException();
        assertEquals("Cannot transfer to the same account", ex.getMessage());
    }

    @Test
    @DisplayName("AccountInactiveException contains account number")
    void accountInactiveMessage() {
        AccountInactiveException ex = new AccountInactiveException("ACC003");
        assertTrue(ex.getMessage().contains("ACC003"));
    }
}
