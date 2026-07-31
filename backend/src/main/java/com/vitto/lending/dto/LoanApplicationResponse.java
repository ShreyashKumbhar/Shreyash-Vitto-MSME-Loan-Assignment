package com.vitto.lending.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoanApplicationResponse {
    private UUID applicationId;
    private UUID businessProfileId;
    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String purpose;
    private String status;
    private Instant createdAt;
}
