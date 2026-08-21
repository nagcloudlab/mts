package com.mts.dto;

import com.mts.enums.TransferMode;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransferRequest {

    @NotBlank(message = "From account number is required")
    private String fromAccountNumber;

    @NotBlank(message = "To account number is required")
    private String toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
    @DecimalMax(value = "1000000.00", message = "Maximum transfer amount is 10,00,000.00")
    private BigDecimal amount;

    @NotNull(message = "Transfer mode is required")
    private TransferMode transferMode;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
