package com.example.savingswallet.infrastructure.persistence.jpa;
import static org.assertj.core.api.Assertions.assertThat;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SavingsGoalJpaAdapterIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    private SavingsGoalJpaAdapter adapter;
    @Autowired
    private SavingsGoalJpaRepository jpaRepository;
    @Autowired
    private SavingsGoalMapper mapper;

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void persistsSavingsGoalThroughTheAdapter() {
        SavingsGoal saved = adapter.save(SavingsGoal.open(1L, 42L, "Vacaciones", money("1000.00")));
        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.userId()).isEqualTo(42L);
        assertThat(saved.name()).isEqualTo("Vacaciones");
        assertThat(saved.targetAmount()).isEqualTo(money("1000.00"));
        assertThat(saved.accumulatedAmount()).isEqualTo(money("0.00"));
        assertThat(saved.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void reconstructsDomainAggregateFromPersistedRow() {
        adapter.save(new SavingsGoal(2L, 42L, "Fondo", money("500.00"), money("200.00")));
        SavingsGoalEntity entity = jpaRepository.findById(2L).orElseThrow();
        assertThat(entity.getName()).isEqualTo("Fondo");
        assertThat(entity.getTargetAmount()).isEqualByComparingTo("500.00");
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("200.00");
        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getStatus()).isEqualTo(SavingsGoalStatus.ACTIVE);
        SavingsGoal reloaded = mapper.toDomain(entity);
        assertThat(reloaded.id()).isEqualTo(2L);
        assertThat(reloaded.name()).isEqualTo("Fondo");
        assertThat(reloaded.targetAmount()).isEqualTo(money("500.00"));
        assertThat(reloaded.accumulatedAmount()).isEqualTo(money("200.00"));
        assertThat(reloaded.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void reconstructsCompletedGoalWithStatusDerivedFromAmounts() {
        adapter.save(new SavingsGoal(3L, 42L, "Completado", money("500.00"), money("500.00")));
        SavingsGoalEntity entity = jpaRepository.findById(3L).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
        SavingsGoal reloaded = mapper.toDomain(entity);
        assertThat(reloaded.status()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    @Test
    void saveOverwritesExistingGoalWithSameId() {
        adapter.save(SavingsGoal.open(4L, 42L, "Original", money("1000.00")));
        adapter.save(SavingsGoal.open(4L, 42L, "Actualizado", money("2000.00")));
        assertThat(jpaRepository.count()).isEqualTo(1L);
        SavingsGoalEntity entity = jpaRepository.findById(4L).orElseThrow();
        assertThat(entity.getName()).isEqualTo("Actualizado");
        assertThat(entity.getTargetAmount()).isEqualByComparingTo("2000.00");
    }
}
