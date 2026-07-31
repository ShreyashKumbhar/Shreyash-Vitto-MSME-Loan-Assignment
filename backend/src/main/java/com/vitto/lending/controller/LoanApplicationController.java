package com.vitto.lending.controller;

import com.vitto.lending.dto.ApiResponse;
import com.vitto.lending.dto.LoanApplicationRequest;
import com.vitto.lending.dto.LoanApplicationResponse;
import com.vitto.lending.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService service;

    @PostMapping("/business-profiles/{businessProfileId}/applications")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> createApplication(
            @PathVariable UUID businessProfileId,
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse response = service.createApplication(businessProfileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getApplication(@PathVariable UUID id) {
        LoanApplicationResponse response = service.getApplication(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
