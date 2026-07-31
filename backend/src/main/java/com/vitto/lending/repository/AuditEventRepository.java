package com.vitto.lending.repository;

import com.vitto.lending.audit.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
}
