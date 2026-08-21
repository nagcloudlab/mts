package com.mts.dto;

import com.mts.enums.AccountType;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountResponse {
    private String accountNumber;
    private String ownerName;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private Boolean isActive;
}
