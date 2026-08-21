package com.mts.unit;

import com.mts.dto.TransferRequest;
import com.mts.enums.TransferMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNIT TESTS — TransferRequest DTO Validation
 *
 * Tests Bean Validation annotations (@NotBlank, @DecimalMin, @DecimalMax, @NotNull)
 * No Spring context needed — uses Jakarta Validator directly.
 */
@DisplayName("Unit: TransferRequest Validation")
@Tag("unit")
class TransferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private TransferRequest validRequest() {
        return TransferRequest.builder()
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("5000"))
                .transferMode(TransferMode.UPI)
                .description("Test transfer")
                .build();
    }

    @Test
    @DisplayName("Valid request should have zero violations")
    void validRequestPassesValidation() {
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(validRequest());
        assertTrue(violations.isEmpty());
    }

    @Nested
    @DisplayName("fromAccountNumber validation")
    class FromAccountValidation {

        @Test
        @DisplayName("Should fail when fromAccountNumber is null")
        void failsWhenNull() {
            TransferRequest req = validRequest();
            req.setFromAccountNumber(null);
            assertFalse(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should fail when fromAccountNumber is blank")
        void failsWhenBlank() {
            TransferRequest req = validRequest();
            req.setFromAccountNumber("  ");
            assertFalse(validator.validate(req).isEmpty());
        }
    }

    @Nested
    @DisplayName("toAccountNumber validation")
    class ToAccountValidation {

        @Test
        @DisplayName("Should fail when toAccountNumber is null")
        void failsWhenNull() {
            TransferRequest req = validRequest();
            req.setToAccountNumber(null);
            assertFalse(validator.validate(req).isEmpty());
        }
    }

    @Nested
    @DisplayName("amount validation")
    class AmountValidation {

        @Test
        @DisplayName("Should fail when amount is null")
        void failsWhenNull() {
            TransferRequest req = validRequest();
            req.setAmount(null);
            assertFalse(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should fail when amount is below minimum (1.00)")
        void failsWhenBelowMin() {
            TransferRequest req = validRequest();
            req.setAmount(new BigDecimal("0.50"));
            assertFalse(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should fail when amount exceeds maximum (1000000.00)")
        void failsWhenAboveMax() {
            TransferRequest req = validRequest();
            req.setAmount(new BigDecimal("1000001"));
            assertFalse(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should pass at exact minimum boundary (1.00)")
        void passesAtMinBoundary() {
            TransferRequest req = validRequest();
            req.setAmount(new BigDecimal("1.00"));
            assertTrue(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should pass at exact maximum boundary (1000000.00)")
        void passesAtMaxBoundary() {
            TransferRequest req = validRequest();
            req.setAmount(new BigDecimal("1000000.00"));
            assertTrue(validator.validate(req).isEmpty());
        }
    }

    @Nested
    @DisplayName("transferMode validation")
    class TransferModeValidation {

        @Test
        @DisplayName("Should fail when transferMode is null")
        void failsWhenNull() {
            TransferRequest req = validRequest();
            req.setTransferMode(null);
            assertFalse(validator.validate(req).isEmpty());
        }
    }

    @Nested
    @DisplayName("description validation")
    class DescriptionValidation {

        @Test
        @DisplayName("Should pass when description is null (optional)")
        void passesWhenNull() {
            TransferRequest req = validRequest();
            req.setDescription(null);
            assertTrue(validator.validate(req).isEmpty());
        }

        @Test
        @DisplayName("Should fail when description exceeds 255 characters")
        void failsWhenTooLong() {
            TransferRequest req = validRequest();
            req.setDescription("x".repeat(256));
            assertFalse(validator.validate(req).isEmpty());
        }
    }
}
