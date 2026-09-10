package com.example.savingswallet.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.savingswallet.application.port.out.DomainEventPublisher;
import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.application.usecase.AddContribution;
import com.example.savingswallet.application.usecase.SavingsGoalNotFoundException;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AddContributionIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    private SavingsGoalRepository repository;
    @Autowired
    private SavingsGoalJpaRepository jpaRepository;
    @Autowired
    private DomainEventPublisher eventPublisher;

    private AddContribution addContribution;

    @BeforeEach
    void setUp() {
        addContribution = new AddContribution(repository, eventPublisher);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private Long createGoal(Long userId, String name, String target) {
        return repository.save(SavingsGoal.open(userId, name, money(target))).id();
    }

    @Test
    void persistsAContribution() {
        Long goalId = createGoal(100L, "Vacaciones", "1000.00");

        addContribution.execute(goalId, 100L, money("150.00"));

        SavingsGoalEntity entity = jpaRepository.findById(goalId).orElseThrow();
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("150.00");
        SavingsGoal reloaded = repository.findByIdAndUserId(goalId, 100L).orElseThrow();
        assertThat(reloaded.accumulatedAmount()).isEqualTo(money("150.00"));
    }

    @Test
    void updatesAccumulatedAmountCorrectly() {
        Long goalId = createGoal(100L, "Fondo", "1000.00");

        SavingsGoal result = addContribution.execute(goalId, 100L, money("50.00"));

        assertThat(result.accumulatedAmount()).isEqualTo(money("50.00"));
        assertThat(jpaRepository.findById(goalId).orElseThrow().getAccumulatedAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void activeGoalRemainsActiveWhenTargetIsNotReached() {
        Long goalId = createGoal(100L, "Fondo", "1000.00");

        SavingsGoal result = addContribution.execute(goalId, 100L, money("100.00"));

        assertThat(result.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void goalBecomesCompletedWhenContributionReachesTarget() {
        Long goalId = createGoal(100L, "Fondo", "1000.00");

        SavingsGoal result = addContribution.execute(goalId, 100L, money("1000.00"));

        assertThat(result.accumulatedAmount()).isEqualTo(money("1000.00"));
        assertThat(result.status()).isEqualTo(SavingsGoalStatus.COMPLETED);
        assertThat(jpaRepository.findById(goalId).orElseThrow().getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    @Test
    void cannotRetrieveOrContributeToGoalOfAnotherUser() {
        Long goalId = createGoal(100L, "Ajena", "1000.00");

        assertThatThrownBy(() -> addContribution.execute(goalId, 200L, money("10.00")))
                .isInstanceOf(SavingsGoalNotFoundException.class);
        SavingsGoalEntity entity = jpaRepository.findById(goalId).orElseThrow();
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void goalNotFoundFails() {
        assertThatThrownBy(() -> addContribution.execute(999L, 100L, money("10.00")))
                .isInstanceOf(SavingsGoalNotFoundException.class);
    }
}
