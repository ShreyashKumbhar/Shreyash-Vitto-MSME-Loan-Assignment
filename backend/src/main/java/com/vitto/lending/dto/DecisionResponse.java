package com.vitto.lending.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DecisionResponse {
    private UUID applicationId;
    private String decision;
    private Integer creditScore;
    private List<String> reasonCodes;
    private ScoreBreakdownDTO scoreBreakdown;
    private Instant decidedAt;
}
