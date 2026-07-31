package com.vitto.lending.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    private String id;

    private String eventType; // "APPLICATION_SUBMITTED" | "DECISION_COMPUTED"

    private String applicationId;

    private String businessProfileId;

    private Map<String, Object> payload;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
