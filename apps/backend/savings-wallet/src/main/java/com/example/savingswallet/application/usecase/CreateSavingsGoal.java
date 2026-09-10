package com.example.savingswallet.application.usecase;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import java.util.Objects;

/**
 * Creates a new {@link SavingsGoal} and persists it through the outbound
 * {@link SavingsGoalRepository} port.
 *
 * <p>Business rules are enforced by the aggregate ({@link SavingsGoal#open}):
 * the use case only receives the data, asks the aggregate to build itself and
 * delegates persistence to the repository port.
 */
public final class CreateSavingsGoal {

    private final SavingsGoalRepository repository;

    public CreateSavingsGoal(SavingsGoalRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public SavingsGoal execute(Long userId, String name, Money targetAmount) {
        SavingsGoal goal = SavingsGoal.open(userId, name, targetAmount);
        return repository.save(goal);
    }
}
