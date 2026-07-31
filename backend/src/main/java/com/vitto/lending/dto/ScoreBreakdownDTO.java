package com.vitto.lending.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDTO {
    private int revenueToEmiPoints;
    private int loanToRevenuePoints;
    private int tenurePoints;
    private int businessTypePoints;
}
