package com.vitto.lending.controller;

import com.vitto.lending.dto.ApiResponse;
import com.vitto.lending.dto.BusinessProfileRequest;
import com.vitto.lending.dto.BusinessProfileResponse;
import com.vitto.lending.service.BusinessProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-profiles")
public class BusinessProfileController {

    @Autowired
    private BusinessProfileService service;

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> createProfile(
            @Valid @RequestBody BusinessProfileRequest request) {
        BusinessProfileResponse response = service.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> getProfile(@PathVariable UUID id) {
        BusinessProfileResponse response = service.getProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
