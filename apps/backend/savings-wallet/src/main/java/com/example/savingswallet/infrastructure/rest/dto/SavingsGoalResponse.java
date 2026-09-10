package com.example.savingswallet.infrastructure.rest.dto;

import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;

/**
 * Response representation of a {@link SavingsGoal}.
 *
 * <p>Monetary values are normalized to scale 2, consistent with the persisted
 * schema and the API contract shared with the frontend.
 */
public record SavingsGoalResponse(
        Long id,
        Long userId,
        String name,
        BigDecimal targetAmount,
        BigDecimal accumulatedAmount,
        String currency,
        SavingsGoalStatus status) {

    public static SavingsGoalResponse from(SavingsGoal goal) {
        return new SavingsGoalResponse(
                goal.id(),
                goal.userId(),
                goal.name(),
                goal.targetAmount().amount().setScale(2),
                goal.accumulatedAmount().amount().setScale(2),
                goal.targetAmount().currency().getCurrencyCode(),
                goal.status());
    }
}