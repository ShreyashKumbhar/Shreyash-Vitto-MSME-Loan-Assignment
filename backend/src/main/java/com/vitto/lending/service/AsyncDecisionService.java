package com.vitto.lending.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncDecisionService {

    @Autowired
    private DecisionEngineService decisionEngineService;

    @Async
    public CompletableFuture<Void> processDecisionAsync(UUID applicationId) {
        try {
            // Simulate processing delay for demo/async visibility
            Thread.sleep(2000);
            decisionEngineService.processDecision(applicationId);
        } catch (Exception e) {
            // Error handling can be enhanced for production
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }
}
