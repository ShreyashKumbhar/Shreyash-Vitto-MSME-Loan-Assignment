package com.vitto.lending.service;

import com.vitto.lending.dto.DecisionResponse;
import com.vitto.lending.dto.ScoreBreakdownDTO;
import com.vitto.lending.entity.BusinessProfile;
import com.vitto.lending.entity.Decision;
import com.vitto.lending.entity.LoanApplication;
import com.vitto.lending.repository.DecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DecisionEngineService {

    private static final BigDecimal FLAT_INTEREST_RATE = new BigDecimal("1.12");
    private static final int APPROVAL_THRESHOLD = 60;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private AuditService auditService;

    @Transactional
    public DecisionResponse processDecision(UUID applicationId) {
        // Idempotency check: if decision exists, return it
        Optional<Decision> existingDecision = decisionRepository.findByLoanApplicationId(applicationId);
        if (existingDecision.isPresent()) {
            return mapToResponse(existingDecision.get());
        }

        LoanApplication application = loanApplicationService.getApplicationEntity(applicationId);
        BusinessProfile profile = application.getBusinessProfile();

        List<String> reasonCodes = new ArrayList<>();
        
        // 1. Revenue-to-EMI Ratio
        BigDecimal monthlyRevenue = profile.getMonthlyRevenue();
        BigDecimal loanAmount = application.getLoanAmount();
        int tenureMonths = application.getTenureMonths();

        // estimatedMonthlyEMI = loanAmount / tenureMonths * 1.12
        BigDecimal tenure = new BigDecimal(tenureMonths);
        BigDecimal estimatedMonthlyEMI = loanAmount.divide(tenure, 2, RoundingMode.HALF_UP)
                .multiply(FLAT_INTEREST_RATE);

        BigDecimal revenueEmiRatio = monthlyRevenue.divide(estimatedMonthlyEMI, 2, RoundingMode.HALF_UP);
        int revenueEmiPoints = 0;

        if (revenueEmiRatio.compareTo(new BigDecimal("3.0")) >= 0) {
            revenueEmiPoints = 40;
            reasonCodes.add("STRONG_REPAYMENT_CAPACITY");
        } else if (revenueEmiRatio.compareTo(new BigDecimal("2.0")) >= 0) {
            revenueEmiPoints = 30;
        } else if (revenueEmiRatio.compareTo(new BigDecimal("1.5")) >= 0) {
            revenueEmiPoints = 18;
        } else if (revenueEmiRatio.compareTo(new BigDecimal("1.0")) >= 0) {
            revenueEmiPoints = 8;
            reasonCodes.add("LOW_REVENUE_TO_EMI");
        } else {
            revenueEmiPoints = 0;
            reasonCodes.add("LOW_REVENUE_TO_EMI");
        }

        // 2. Loan-to-revenue multiple
        BigDecimal loanMultiple = loanAmount.divide(monthlyRevenue, 2, RoundingMode.HALF_UP);
        int loanMultiplePoints = 0;

        if (loanMultiple.compareTo(new BigDecimal("3.0")) <= 0) {
            loanMultiplePoints = 25;
            reasonCodes.add("HEALTHY_LOAN_SIZE");
        } else if (loanMultiple.compareTo(new BigDecimal("6.0")) <= 0) {
            loanMultiplePoints = 18;
        } else if (loanMultiple.compareTo(new BigDecimal("10.0")) <= 0) {
            loanMultiplePoints = 10;
            reasonCodes.add("HIGH_LOAN_RATIO");
        } else if (loanMultiple.compareTo(new BigDecimal("15.0")) <= 0) {
            loanMultiplePoints = 4;
            reasonCodes.add("HIGH_LOAN_RATIO");
        } else {
            loanMultiplePoints = 0;
            reasonCodes.add("HIGH_LOAN_RATIO");
        }

        // 3. Tenure risk
        int tenurePoints = 0;
        if (tenureMonths < 3) {
            tenurePoints = 5;
            reasonCodes.add("SHORT_TENURE_RISK");
        } else if (tenureMonths <= 6) {
            tenurePoints = 10;
        } else if (tenureMonths <= 36) {
            tenurePoints = 15;
        } else if (tenureMonths <= 60) {
            tenurePoints = 10;
        } else {
            tenurePoints = 5;
            reasonCodes.add("LONG_TENURE_RISK");
        }

        // 4. Business type risk
        int businessTypePoints = 0;
        String type = profile.getBusinessType().toLowerCase();
        if ("services".equals(type)) {
            businessTypePoints = 10;
        } else if ("retail".equals(type)) {
            businessTypePoints = 7;
        } else if ("manufacturing".equals(type)) {
            businessTypePoints = 5;
            reasonCodes.add("SECTOR_RISK");
        }

        // 5. Calculate Total
        int totalScore = revenueEmiPoints + loanMultiplePoints + tenurePoints + businessTypePoints;
        boolean isFraudCapTriggered = false;

        // Consistency / fraud gate
        if (loanAmount.compareTo(monthlyRevenue.multiply(new BigDecimal("20"))) > 0) {
            isFraudCapTriggered = true;
            reasonCodes.add("DATA_INCONSISTENCY");
            totalScore = Math.min(totalScore, 20);
        }

        String finalDecision = (totalScore >= APPROVAL_THRESHOLD && !isFraudCapTriggered) ? "Approved" : "Rejected";

        ScoreBreakdownDTO breakdown = ScoreBreakdownDTO.builder()
                .revenueToEmiPoints(revenueEmiPoints)
                .loanToRevenuePoints(loanMultiplePoints)
                .tenurePoints(tenurePoints)
                .businessTypePoints(businessTypePoints)
                .build();

        Decision decision = Decision.builder()
                .loanApplication(application)
                .decision(finalDecision)
                .creditScore(totalScore)
                .reasonCodes(reasonCodes)
                .scoreBreakdown(breakdown)
                .build();

        decision = decisionRepository.save(decision);

        // Update application status
        application.setStatus("DECISIONED");
        
        DecisionResponse response = mapToResponse(decision);
        
        // Log event asynchronously
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("decision", response.getDecision());
        payload.put("creditScore", response.getCreditScore());
        
        com.vitto.lending.audit.AuditEvent auditEvent = com.vitto.lending.audit.AuditEvent.builder()
                .eventType("DECISION_COMPUTED")
                .applicationId(response.getApplicationId().toString())
                .businessProfileId(application.getBusinessProfile().getId().toString())
                .payload(payload)
                .build();
                
        auditService.logEvent(auditEvent);
        
        return response;
    }

    public DecisionResponse getDecision(UUID applicationId) {
        Decision decision = decisionRepository.findByLoanApplicationId(applicationId)
                .orElse(null); // The controller should handle if not found, or maybe polling handles it.
        if (decision == null) {
            return null;
        }
        return mapToResponse(decision);
    }

    private DecisionResponse mapToResponse(Decision decision) {
        return DecisionResponse.builder()
                .applicationId(decision.getLoanApplication().getId())
                .decision(decision.getDecision())
                .creditScore(decision.getCreditScore())
                .reasonCodes(decision.getReasonCodes())
                .scoreBreakdown(decision.getScoreBreakdown())
                .decidedAt(decision.getDecidedAt())
                .build();
    }
}
