package com.vitto.lending.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BusinessProfileRequest {
    
    @NotBlank(message = "is required")
    private String ownerName;

    @NotBlank(message = "is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "does not match required PAN format")
    private String pan;

    @NotBlank(message = "is required")
    @Pattern(regexp = "^(retail|manufacturing|services)$", message = "must be retail, manufacturing, or services")
    private String businessType;

    @NotNull(message = "is required")
    @Positive(message = "must be greater than 0")
    private BigDecimal monthlyRevenue;
}
