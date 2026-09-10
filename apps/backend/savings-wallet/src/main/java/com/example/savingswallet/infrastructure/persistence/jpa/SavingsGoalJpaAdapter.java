package com.example.savingswallet.infrastructure.persistence.jpa;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of the {@link SavingsGoalRepository} outbound port.
 */
@Component
public class SavingsGoalJpaAdapter implements SavingsGoalRepository {

    private final SavingsGoalJpaRepository jpaRepository;
    private final SavingsGoalMapper mapper;

    public SavingsGoalJpaAdapter(SavingsGoalJpaRepository jpaRepository, SavingsGoalMapper mapper) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SavingsGoal save(SavingsGoal goal) {
        SavingsGoalEntity entity = mapper.toEntity(goal);
        SavingsGoalEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<SavingsGoal> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SavingsGoal> findByIdAndUserId(Long goalId, Long userId) {
        return jpaRepository.findByIdAndUserId(goalId, userId)
                .map(mapper::toDomain);
    }
}
