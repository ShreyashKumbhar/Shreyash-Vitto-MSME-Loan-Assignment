package com.vitto.lending.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BusinessProfileResponse {
    private UUID businessProfileId;
    private String ownerName;
    private String pan;
    private String businessType;
    private BigDecimal monthlyRevenue;
    private Instant createdAt;
}
