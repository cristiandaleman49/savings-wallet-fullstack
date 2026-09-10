package com.example.savingswallet.application.usecase;

import com.example.savingswallet.application.port.out.DomainEventPublisher;
import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import java.util.Objects;

/**
 * Registers a contribution to a {@link SavingsGoal}.
 *
 * <p>The use case only orchestrates the operation: it validates the identifiers,
 * retrieves the goal through the repository using both {@code goalId} and
 * {@code userId} (so the ownership boundary is enforced while loading), delegates
 * the contribution to the aggregate, persists the updated aggregate and finally
 * publishes any domain events the contribution raised.
 *
 * <p>All contribution business rules (positive amount, target not exceeded,
 * completed goal rejection, ACTIVE to COMPLETED transition) live in
 * {@link SavingsGoal#contribute}; the aggregate also decides when a
 * {@code GoalCompleted} event is raised. Events are published only after the
 * aggregate has been persisted.
 */
public final class AddContribution {

    private final SavingsGoalRepository repository;
    private final DomainEventPublisher eventPublisher;

    public AddContribution(SavingsGoalRepository repository, DomainEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public SavingsGoal execute(Long goalId, Long userId, Money contribution) {
        Objects.requireNonNull(goalId, "goalId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(contribution, "contribution must not be null");
        SavingsGoal goal = repository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new SavingsGoalNotFoundException(goalId, userId));
        goal.contribute(contribution);
        SavingsGoal saved = repository.save(goal);
        goal.domainEvents().forEach(eventPublisher::publish);
        return saved;
    }
}