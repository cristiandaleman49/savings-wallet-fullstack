package com.example.savingswallet.domain.event;

import com.example.savingswallet.domain.money.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * Raised when a {@code SavingsGoal} reaches its target amount through a
 * contribution and transitions to {@code COMPLETED}.
 *
 * @param goalId       identifier of the completed goal
 * @param userId       owner of the completed goal
 * @param goalName     name of the completed goal
 * @param targetAmount target amount that was reached
 * @param completedAt  moment the goal reached its target
 */
public record GoalCompleted(
        Long goalId,
        Long userId,
        String goalName,
        Money targetAmount,
        Instant completedAt) implements DomainEvent {

    public GoalCompleted {
        Objects.requireNonNull(goalId, "goalId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(goalName, "goalName must not be null");
        Objects.requireNonNull(targetAmount, "targetAmount must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    @Override
    public Instant occurredAt() {
        return completedAt;
    }
}