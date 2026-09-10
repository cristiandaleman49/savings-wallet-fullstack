package com.example.savingswallet.domain.savingsgoal;

import com.example.savingswallet.domain.money.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregate root that protects the invariants of a savings goal.
 *
 * <p>The goal accumulates money toward a target. It starts {@code ACTIVE}, and
 * becomes {@code COMPLETED} as soon as the accumulated amount reaches the target
 * amount. A completed goal can no longer receive contributions.
 */
public final class SavingsGoal {

    private final Long id;
    private final Long userId;
    private final String name;
    private final Money targetAmount;
    private Money accumulatedAmount;
    private SavingsGoalStatus status;

    /**
     * Creates a goal from its current snapshot, deriving the status from the
     * accumulated vs. target amounts. Useful for rehydrating a persisted goal
     * as well as for testing the completion invariant.
     */
    public SavingsGoal(Long id, Long userId, String name, Money targetAmount, Money accumulatedAmount) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.name = requireValidName(name);
        this.targetAmount = requirePositive(Objects.requireNonNull(targetAmount, "targetAmount must not be null"), "targetAmount");
        this.accumulatedAmount = Objects.requireNonNull(accumulatedAmount, "accumulatedAmount must not be null");
        requireSameCurrency(targetAmount, accumulatedAmount, "targetAmount and accumulatedAmount");
        if (accumulatedAmount.amount().compareTo(targetAmount.amount()) > 0) {
            throw new IllegalArgumentException("accumulatedAmount must not exceed targetAmount");
        }
        this.status = accumulatedAmount.amount().compareTo(targetAmount.amount()) >= 0
                ? SavingsGoalStatus.COMPLETED
                : SavingsGoalStatus.ACTIVE;
    }

    /**
     * Opens a new goal with nothing accumulated yet; it always starts
     * {@code ACTIVE}.
     */
    public static SavingsGoal open(Long id, Long userId, String name, Money targetAmount) {
        Objects.requireNonNull(targetAmount, "targetAmount must not be null");
        return new SavingsGoal(id, userId, name, targetAmount, new Money(BigDecimal.ZERO, targetAmount.currency()));
    }

    /**
     * Records a contribution toward the target.
     *
     * <p>Rejects contributions when the goal is already {@code COMPLETED} or
     * when the contribution would push the accumulated amount beyond the
     * target. When the accumulated amount reaches (not exceeds) the target,
     * the goal transitions to {@code COMPLETED}.
     */
    public void contribute(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (status == SavingsGoalStatus.COMPLETED) {
            throw new IllegalStateException("cannot contribute to a COMPLETED goal");
        }
        requireSameCurrency(targetAmount, amount, "contribution and targetAmount");
        BigDecimal newAccumulatedAmount = accumulatedAmount.amount().add(amount.amount());
        if (newAccumulatedAmount.compareTo(targetAmount.amount()) > 0) {
            throw new IllegalArgumentException("contribution would make accumulatedAmount exceed targetAmount");
        }
        accumulatedAmount = new Money(newAccumulatedAmount, targetAmount.currency());
        if (accumulatedAmount.amount().compareTo(targetAmount.amount()) >= 0) {
            status = SavingsGoalStatus.COMPLETED;
        }
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public Money targetAmount() {
        return targetAmount;
    }

    public Money accumulatedAmount() {
        return accumulatedAmount;
    }

    public SavingsGoalStatus status() {
        return status;
    }

    private static String requireValidName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    private static Money requirePositive(Money money, String label) {
        if (money.amount().signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
        return money;
    }

    private static void requireSameCurrency(Money first, Money second, String parts) {
        if (!first.currency().equals(second.currency())) {
            throw new IllegalArgumentException(parts + " must have the same currency");
        }
    }
}