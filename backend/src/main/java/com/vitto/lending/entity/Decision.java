package com.vitto.lending.entity;

import com.vitto.lending.dto.ScoreBreakdownDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Column(name = "decision", nullable = false, length = 10)
    private String decision; // "Approved" | "Rejected"

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "reason_codes", nullable = false, columnDefinition = "text[]")
    private List<String> reasonCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown", nullable = false, columnDefinition = "jsonb")
    private ScoreBreakdownDTO scoreBreakdown;

    @Builder.Default
    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (decidedAt == null) {
            decidedAt = Instant.now();
        }
    }
}
