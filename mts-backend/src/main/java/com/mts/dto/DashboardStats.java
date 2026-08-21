package com.mts.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DashboardStats {
    private long totalAccounts;
    private long activeAccounts;
    private BigDecimal totalBalance;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private BigDecimal totalTransferred;
}
