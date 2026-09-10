package com.example.savingswallet.infrastructure.persistence.jpa;

import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;

import java.util.Currency;
import org.springframework.stereotype.Component;

/**
 * Maps between the {@link SavingsGoal} aggregate and its JPA {@link SavingsGoalEntity}.
 *
 * <p>Reconstruction goes through the aggregate constructor so the domain invariants
 * (including status derivation from the amounts) are re-applied on every read;
 * the persisted status column is therefore only used for querying, never trusted
 * for the in-memory aggregate state.
 */
@Component
public class SavingsGoalMapper {

    public SavingsGoalEntity toEntity(SavingsGoal goal) {
        SavingsGoalEntity entity = new SavingsGoalEntity();
        entity.setId(goal.id());
        entity.setUserId(goal.userId());
        entity.setName(goal.name());
        entity.setTargetAmount(goal.targetAmount().amount());
        entity.setAccumulatedAmount(goal.accumulatedAmount().amount());
        entity.setCurrency(goal.targetAmount().currency().getCurrencyCode());
        entity.setStatus(goal.status());
        return entity;
    }

    public SavingsGoal toDomain(SavingsGoalEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        return new SavingsGoal(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                new Money(entity.getTargetAmount(), currency),
                new Money(entity.getAccumulatedAmount(), currency)
        );
    }
}
