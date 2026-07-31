package com.vitto.lending.service;

import com.vitto.lending.dto.BusinessProfileRequest;
import com.vitto.lending.dto.BusinessProfileResponse;
import com.vitto.lending.entity.BusinessProfile;
import com.vitto.lending.exception.ResourceNotFoundException;
import com.vitto.lending.repository.BusinessProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BusinessProfileService {

    @Autowired
    private BusinessProfileRepository repository;

    public BusinessProfileResponse createProfile(BusinessProfileRequest request) {
        BusinessProfile profile = BusinessProfile.builder()
                .ownerName(request.getOwnerName())
                .pan(request.getPan())
                .businessType(request.getBusinessType())
                .monthlyRevenue(request.getMonthlyRevenue())
                .build();
        
        profile = repository.save(profile);
        return mapToResponse(profile);
    }

    public BusinessProfileResponse getProfile(UUID id) {
        BusinessProfile profile = getProfileEntity(id);
        return mapToResponse(profile);
    }

    public BusinessProfile getProfileEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found with id: " + id));
    }

    private BusinessProfileResponse mapToResponse(BusinessProfile profile) {
        return BusinessProfileResponse.builder()
                .businessProfileId(profile.getId())
                .ownerName(profile.getOwnerName())
                .pan(profile.getPan())
                .businessType(profile.getBusinessType())
                .monthlyRevenue(profile.getMonthlyRevenue())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
