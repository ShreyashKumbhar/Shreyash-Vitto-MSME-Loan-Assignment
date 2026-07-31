package com.vitto.lending.controller;

import com.vitto.lending.dto.ApiResponse;
import com.vitto.lending.dto.DecisionResponse;
import com.vitto.lending.entity.LoanApplication;
import com.vitto.lending.service.AsyncDecisionService;
import com.vitto.lending.service.DecisionEngineService;
import com.vitto.lending.service.LoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{id}/decision")
public class DecisionController {

    @Autowired
    private DecisionEngineService decisionEngineService;

    @Autowired
    private AsyncDecisionService asyncDecisionService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createDecision(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "sync") String mode) {
        
        // Ensure application exists
        loanApplicationService.getApplicationEntity(id);

        if ("async".equalsIgnoreCase(mode)) {
            DecisionResponse existing = decisionEngineService.getDecision(id);
            if (existing != null) {
                return ResponseEntity.ok(ApiResponse.success(existing));
            }

            asyncDecisionService.processDecisionAsync(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("applicationId", id.toString());
            response.put("status", "PROCESSING");
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
        } else {
            DecisionResponse response = decisionEngineService.processDecision(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getDecision(@PathVariable UUID id) {
        // Ensure application exists
        LoanApplication application = loanApplicationService.getApplicationEntity(id);

        DecisionResponse existing = decisionEngineService.getDecision(id);
        
        if (existing != null) {
            return ResponseEntity.ok(ApiResponse.success(existing));
        } else if ("SUBMITTED".equals(application.getStatus())) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PROCESSING");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
        } else {
            // Should theoretically not happen unless data is in weird state
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PENDING");
            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }
}
