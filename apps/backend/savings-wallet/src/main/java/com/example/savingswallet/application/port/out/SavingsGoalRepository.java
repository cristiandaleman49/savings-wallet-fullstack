package com.example.savingswallet.application.port.out;

import com.example.savingswallet.domain.savingsgoal.SavingsGoal;

/**
 * Outbound port for persisting {@link SavingsGoal} aggregates.
 *
 * <p>The application depends on this abstraction; concrete implementations
 * live in the adapters/infrastructure layer and must not leak framework or
 * persistence concerns into the application.
 */
public interface SavingsGoalRepository {

    SavingsGoal save(SavingsGoal goal);
}
