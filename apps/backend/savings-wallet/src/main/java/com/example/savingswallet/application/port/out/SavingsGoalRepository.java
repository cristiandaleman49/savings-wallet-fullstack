package com.example.savingswallet.application.port.out;

import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for persisting and querying {@link SavingsGoal} aggregates.
 *
 * <p>The application depends on this abstraction; concrete implementations
 * live in the adapters/infrastructure layer and must not leak framework or
 * persistence concerns into the application.
 */
public interface SavingsGoalRepository {

    SavingsGoal save(SavingsGoal goal);

    List<SavingsGoal> findByUserId(Long userId);

    /**
     * Retrieves the goal identified by {@code goalId} only if it belongs to
     * {@code userId}, enforcing the ownership boundary inside the repository
     * so goals of other users are never exposed.
     */
    Optional<SavingsGoal> findByIdAndUserId(Long goalId, Long userId);
}
