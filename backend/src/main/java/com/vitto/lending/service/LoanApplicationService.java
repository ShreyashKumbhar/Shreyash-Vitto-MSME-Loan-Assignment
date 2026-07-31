package com.vitto.lending.service;

import com.vitto.lending.dto.LoanApplicationRequest;
import com.vitto.lending.dto.LoanApplicationResponse;
import com.vitto.lending.entity.BusinessProfile;
import com.vitto.lending.entity.LoanApplication;
import com.vitto.lending.exception.ResourceNotFoundException;
import com.vitto.lending.repository.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private BusinessProfileService businessProfileService;

    @Autowired
    private AuditService auditService;

    public LoanApplicationResponse createApplication(UUID businessProfileId, LoanApplicationRequest request) {
        BusinessProfile profile = businessProfileService.getProfileEntity(businessProfileId);

        LoanApplication application = LoanApplication.builder()
                .businessProfile(profile)
                .loanAmount(request.getLoanAmount())
                .tenureMonths(request.getTenureMonths())
                .purpose(request.getPurpose())
                .build();
        
        application = repository.save(application);
        
        LoanApplicationResponse response = mapToResponse(application);
        
        // Asynchronously log to Mongo Audit Trail
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("loanAmount", response.getLoanAmount());
        payload.put("tenureMonths", response.getTenureMonths());
        
        com.vitto.lending.audit.AuditEvent auditEvent = com.vitto.lending.audit.AuditEvent.builder()
                .eventType("APPLICATION_SUBMITTED")
                .applicationId(response.getApplicationId().toString())
                .businessProfileId(response.getBusinessProfileId().toString())
                .payload(payload)
                .build();
                
        auditService.logEvent(auditEvent);
        
        return response;
    }

    public LoanApplicationResponse getApplication(UUID id) {
        LoanApplication application = getApplicationEntity(id);
        return mapToResponse(application);
    }

    public LoanApplication getApplicationEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));
    }

    private LoanApplicationResponse mapToResponse(LoanApplication application) {
        return LoanApplicationResponse.builder()
                .applicationId(application.getId())
                .businessProfileId(application.getBusinessProfile().getId())
                .loanAmount(application.getLoanAmount())
                .tenureMonths(application.getTenureMonths())
                .purpose(application.getPurpose())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
