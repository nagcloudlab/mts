package com.mts.dto;

import com.mts.enums.TransactionStatus;
import com.mts.enums.TransferMode;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransferResponse {
    private String referenceId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private TransferMode transferMode;
    private TransactionStatus status;
    private String description;
    private LocalDateTime timestamp;
}
