package com.vitto.lending.service;

import com.vitto.lending.dto.DecisionResponse;
import com.vitto.lending.entity.BusinessProfile;
import com.vitto.lending.entity.Decision;
import com.vitto.lending.entity.LoanApplication;
import com.vitto.lending.repository.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DecisionEngineServiceTest {

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private LoanApplicationService loanApplicationService;

    @InjectMocks
    private DecisionEngineService decisionEngineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock save to just return the saved entity
        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(decisionRepository.findByLoanApplicationId(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void testWorkedExample_ShouldScore90AndApprove() {
        // Business: services, monthly revenue ₹4,00,000. Loan: ₹8,00,000, tenure 12 months.
        UUID appId = UUID.randomUUID();
        BusinessProfile profile = BusinessProfile.builder()
                .businessType("services")
                .monthlyRevenue(new BigDecimal("400000"))
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(appId)
                .businessProfile(profile)
                .loanAmount(new BigDecimal("800000"))
                .tenureMonths(12)
                .build();

        when(loanApplicationService.getApplicationEntity(appId)).thenReturn(application);

        DecisionResponse response = decisionEngineService.processDecision(appId);

        assertNotNull(response);
        assertEquals("Approved", response.getDecision());
        assertEquals(90, response.getCreditScore());
        
        // Check breakdown points
        assertEquals(40, response.getScoreBreakdown().getRevenueToEmiPoints());
        assertEquals(25, response.getScoreBreakdown().getLoanToRevenuePoints());
        assertEquals(15, response.getScoreBreakdown().getTenurePoints());
        assertEquals(10, response.getScoreBreakdown().getBusinessTypePoints());
        
        assertTrue(response.getReasonCodes().contains("STRONG_REPAYMENT_CAPACITY"));
        assertTrue(response.getReasonCodes().contains("HEALTHY_LOAN_SIZE"));
    }

    @Test
    void testBorderlineApproval_ShouldScoreJustAbove60() {
        UUID appId = UUID.randomUUID();
        BusinessProfile profile = BusinessProfile.builder()
                .businessType("manufacturing") // 5 pts
                .monthlyRevenue(new BigDecimal("100000"))
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(appId)
                .businessProfile(profile)
                .loanAmount(new BigDecimal("600000")) // Multiple is 6 -> 18 pts
                .tenureMonths(24) // 15 pts
                .build();
        // EMI = 600000 / 24 * 1.12 = 28000. Ratio = 100000 / 28000 = 3.57 -> 40 pts
        // Total = 5 + 18 + 15 + 40 = 78 (Approved)

        when(loanApplicationService.getApplicationEntity(appId)).thenReturn(application);

        DecisionResponse response = decisionEngineService.processDecision(appId);

        assertEquals("Approved", response.getDecision());
        assertEquals(78, response.getCreditScore());
    }

    @Test
    void testClearRejection_ShouldFail() {
        UUID appId = UUID.randomUUID();
        BusinessProfile profile = BusinessProfile.builder()
                .businessType("retail") // 7 pts
                .monthlyRevenue(new BigDecimal("50000"))
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(appId)
                .businessProfile(profile)
                .loanAmount(new BigDecimal("600000")) // Multiple 12 -> 4 pts
                .tenureMonths(6) // 10 pts
                .build();
        // EMI = 600000 / 6 * 1.12 = 112000. Ratio = 50000 / 112000 = 0.44 -> 0 pts
        // Total = 7 + 4 + 10 + 0 = 21 (Rejected)

        when(loanApplicationService.getApplicationEntity(appId)).thenReturn(application);

        DecisionResponse response = decisionEngineService.processDecision(appId);

        assertEquals("Rejected", response.getDecision());
        assertEquals(21, response.getCreditScore());
        assertTrue(response.getReasonCodes().contains("LOW_REVENUE_TO_EMI"));
        assertTrue(response.getReasonCodes().contains("HIGH_LOAN_RATIO"));
    }

    @Test
    void testConsistencyGate_ShouldCapScoreAndReject() {
        // ₹10L revenue / ₹5Cr loan
        UUID appId = UUID.randomUUID();
        BusinessProfile profile = BusinessProfile.builder()
                .businessType("services") // 10 pts
                .monthlyRevenue(new BigDecimal("1000000")) // 10L
                .build();
        LoanApplication application = LoanApplication.builder()
                .id(appId)
                .businessProfile(profile)
                .loanAmount(new BigDecimal("50000000")) // 5Cr (50x multiple -> 0 pts)
                .tenureMonths(60) // 10 pts
                .build();
        // EMI = 50,000,000 / 60 * 1.12 = 933,333. Ratio = 1,000,000 / 933,333 = 1.07 -> 8 pts
        // Total calculated = 10 + 0 + 10 + 8 = 28
        // BUT 50,000,000 > 20 * 1,000,000, so it triggers consistency gate, capping at 20.

        when(loanApplicationService.getApplicationEntity(appId)).thenReturn(application);

        DecisionResponse response = decisionEngineService.processDecision(appId);

        assertEquals("Rejected", response.getDecision());
        assertEquals(20, response.getCreditScore());
        assertTrue(response.getReasonCodes().contains("DATA_INCONSISTENCY"));
    }
}
