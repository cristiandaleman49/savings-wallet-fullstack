package com.example.savingswallet.infrastructure.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Internal Spring Data JPA repository for {@link SavingsGoalEntity}.
 *
 * <p>This is an infrastructure implementation detail, not an application port.
 * Application code depends on
 * {@link com.example.savingswallet.application.port.out.SavingsGoalRepository}.
 */
public interface SavingsGoalJpaRepository extends JpaRepository<SavingsGoalEntity, Long> {

    List<SavingsGoalEntity> findByUserId(Long userId);
}
