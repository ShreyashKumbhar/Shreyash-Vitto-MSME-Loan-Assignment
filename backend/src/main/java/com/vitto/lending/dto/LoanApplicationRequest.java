package com.vitto.lending.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    
    @NotNull(message = "is required")
    @Positive(message = "must be greater than 0")
    private BigDecimal loanAmount;

    @NotNull(message = "is required")
    @Positive(message = "must be greater than 0")
    private Integer tenureMonths;

    @NotBlank(message = "is required")
    private String purpose;
}
