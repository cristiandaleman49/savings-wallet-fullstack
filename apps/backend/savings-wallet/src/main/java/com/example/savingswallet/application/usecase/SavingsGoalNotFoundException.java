package com.example.savingswallet.application.usecase;

/**
 * Thrown when a {@code SavingsGoal} cannot be found for the requested
 * {@code goalId} and {@code userId} combination.
 *
 * <p>Minimal, purpose-specific exception introduced only for the
 * "goal not found" case of {@link AddContribution}; the ownership boundary is
 * enforced when the aggregate is loaded, so this exception also covers the case
 * where the goal exists but belongs to a different user.
 */
public class SavingsGoalNotFoundException extends RuntimeException {

    public SavingsGoalNotFoundException(Long goalId, Long userId) {
        super("Savings goal with id " + goalId + " was not found for user " + userId);
    }
}