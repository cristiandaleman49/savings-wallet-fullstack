package com.example.savingswallet.infrastructure.config;

import com.example.savingswallet.application.port.out.DomainEventPublisher;
import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.application.usecase.AddContribution;
import com.example.savingswallet.application.usecase.CreateSavingsGoal;
import com.example.savingswallet.application.usecase.GetSavingsGoals;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root wiring for the application use cases.
 *
 * <p>Keeps the application layer free of Spring annotations: these plain classes
 * are exposed as beans here, in the infrastructure layer, and get the outbound
 * {@link SavingsGoalRepository} port (the JPA adapter) injected.
 */
@Configuration
public class SavingsGoalUseCaseConfig {

    @Bean
    public GetSavingsGoals getSavingsGoals(SavingsGoalRepository repository) {
        return new GetSavingsGoals(repository);
    }

    @Bean
    public CreateSavingsGoal createSavingsGoal(SavingsGoalRepository repository) {
        return new CreateSavingsGoal(repository);
    }

    @Bean
    public AddContribution addContribution(SavingsGoalRepository repository, DomainEventPublisher eventPublisher) {
        return new AddContribution(repository, eventPublisher);
    }
}