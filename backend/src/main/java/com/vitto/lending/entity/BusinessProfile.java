package com.vitto.lending.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pan", nullable = false, length = 10)
    private String pan;

    @Column(name = "business_type", nullable = false, length = 20)
    private String businessType;

    @Column(name = "monthly_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyRevenue;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
