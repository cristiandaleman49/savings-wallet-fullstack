package com.example.savingswallet.application.usecase;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import java.util.List;
import java.util.Objects;

/**
 * Returns the {@link SavingsGoal}s belonging to a user by delegating the
 * query to the outbound {@link SavingsGoalRepository} port.
 *
 * <p>The use case only orchestrates the operation; the user-scoped query is
 * enforced by the repository so goals of other users are never exposed.
 */
public final class GetSavingsGoals {

    private final SavingsGoalRepository repository;

    public GetSavingsGoals(SavingsGoalRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public List<SavingsGoal> execute(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return repository.findByUserId(userId);
    }
}