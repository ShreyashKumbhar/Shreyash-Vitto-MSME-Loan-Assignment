package com.vitto.lending.repository;

import com.vitto.lending.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    Optional<Decision> findByLoanApplicationId(UUID applicationId);
}
