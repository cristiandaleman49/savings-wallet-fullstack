package com.example.savingswallet.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private AddContribution addContribution;

    @BeforeEach
    void setUp() {
        addContribution = new AddContribution(repository);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private SavingsGoal createGoal(Long id, Long userId, String name, String target, String accumulated) {
        return repository.save(new SavingsGoal(id, userId, name, money(target), money(accumulated)));
    }

    @Test
    void persistsAContribution() {
        createGoal(20L, 100L, "Vacaciones", "1000.00", "0.00");

        addContribution.execute(20L, 100L, money("150.00"));

        SavingsGoalEntity entity = jpaRepository.findById(20L).orElseThrow();
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("150.00");
        SavingsGoal reloaded = repository.findByIdAndUserId(20L, 100L).orElseThrow();
        assertThat(reloaded.accumulatedAmount()).isEqualTo(money("150.00"));
    }

    @Test
    void updatesAccumulatedAmountCorrectly() {
        createGoal(21L, 100L, "Fondo", "1000.00", "100.00");

        SavingsGoal result = addContribution.execute(21L, 100L, money("50.00"));

        assertThat(result.accumulatedAmount()).isEqualTo(money("150.00"));
        assertThat(jpaRepository.findById(21L).orElseThrow().getAccumulatedAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void activeGoalRemainsActiveWhenTargetIsNotReached() {
        createGoal(22L, 100L, "Fondo", "1000.00", "0.00");

        SavingsGoal result = addContribution.execute(22L, 100L, money("100.00"));

        assertThat(result.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void goalBecomesCompletedWhenContributionReachesTarget() {
        createGoal(23L, 100L, "Fondo", "1000.00", "900.00");

        SavingsGoal result = addContribution.execute(23L, 100L, money("100.00"));

        assertThat(result.accumulatedAmount()).isEqualTo(money("1000.00"));
        assertThat(result.status()).isEqualTo(SavingsGoalStatus.COMPLETED);
        assertThat(jpaRepository.findById(23L).orElseThrow().getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    @Test
    void cannotRetrieveOrContributeToGoalOfAnotherUser() {
        createGoal(24L, 100L, "Ajena", "1000.00", "0.00");

        assertThatThrownBy(() -> addContribution.execute(24L, 200L, money("10.00")))
                .isInstanceOf(SavingsGoalNotFoundException.class);
        SavingsGoalEntity entity = jpaRepository.findById(24L).orElseThrow();
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void goalNotFoundFails() {
        assertThatThrownBy(() -> addContribution.execute(999L, 100L, money("10.00")))
                .isInstanceOf(SavingsGoalNotFoundException.class);
    }
}