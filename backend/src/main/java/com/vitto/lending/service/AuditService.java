package com.vitto.lending.service;

import com.vitto.lending.audit.AuditEvent;
import com.vitto.lending.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired(required = false)
    private AuditEventRepository repository;

    @Async
    public void logEvent(AuditEvent event) {
        if (repository == null) {
            // Used mostly in tests when Mongo is disabled
            return;
        }
        try {
            repository.save(event);
        } catch (Exception e) {
            // Fire-and-forget: do not block the main transaction if Mongo is down
            System.err.println("Failed to write audit event to MongoDB: " + e.getMessage());
        }
    }
}
