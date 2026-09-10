package com.example.savingswallet.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence integration tests against the real SQLite database.
 *
 * <p>With {@code GenerationType.IDENTITY} the identity contract is: a new entity
 * reaches {@code save()} with {@code id == null} (persist + INSERT, SQLite
 * generates the id), and an entity with a non-null id is by definition an
 * existing row (merge + UPDATE). Test data is therefore created through the
 * aggregate's transient factory, exactly like production code does.
 */
@SpringBootTest
@Transactional
class SavingsGoalJpaAdapterIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    private SavingsGoalJpaAdapter adapter;
    @Autowired
    private SavingsGoalJpaRepository jpaRepository;

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void persistsTransientGoalWithDatabaseGeneratedId() {
        SavingsGoal transientGoal = SavingsGoal.open(42L, "Vacaciones", money("1000.00"));
        assertThat(transientGoal.id()).isNull();

        SavingsGoal saved = adapter.save(transientGoal);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.userId()).isEqualTo(42L);
        assertThat(saved.name()).isEqualTo("Vacaciones");
        assertThat(saved.targetAmount()).isEqualTo(money("1000.00"));
        assertThat(saved.accumulatedAmount()).isEqualTo(money("0.00"));
        assertThat(saved.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
        assertThat(jpaRepository.findById(saved.id())).isPresent();
    }

    @Test
    void findByIdAndUserIdReconstructsTheAggregate() {
        SavingsGoal saved = adapter.save(SavingsGoal.open(42L, "Fondo", money("500.00")));

        Optional<SavingsGoal> reloaded = adapter.findByIdAndUserId(saved.id(), 42L);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().id()).isEqualTo(saved.id());
        assertThat(reloaded.get().userId()).isEqualTo(42L);
        assertThat(reloaded.get().name()).isEqualTo("Fondo");
        assertThat(reloaded.get().targetAmount()).isEqualTo(money("500.00"));
        assertThat(reloaded.get().accumulatedAmount()).isEqualTo(money("0.00"));
        assertThat(reloaded.get().status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void findByIdAndUserIdReturnsEmptyForAnotherUser() {
        SavingsGoal saved = adapter.save(SavingsGoal.open(42L, "Ajena", money("500.00")));

        assertThat(adapter.findByIdAndUserId(saved.id(), 43L)).isEmpty();
    }

    @Test
    void savingAnExistingAggregateUpdatesTheRowWithoutCreatingANewOne() {
        SavingsGoal saved = adapter.save(SavingsGoal.open(42L, "Fondo", money("500.00")));

        SavingsGoal loaded = adapter.findByIdAndUserId(saved.id(), 42L).orElseThrow();
        loaded.contribute(money("200.00"));
        SavingsGoal savedAgain = adapter.save(loaded);

        assertThat(savedAgain.id()).isEqualTo(saved.id());
        assertThat(jpaRepository.count()).isEqualTo(1L);
        SavingsGoalEntity entity = jpaRepository.findById(saved.id()).orElseThrow();
        assertThat(entity.getAccumulatedAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void findByUserIdReturnsOnlyGoalsBelongingToThatUser() {
        adapter.save(SavingsGoal.open(100L, "Meta propia", money("1000.00")));
        adapter.save(SavingsGoal.open(200L, "Meta de otro", money("500.00")));

        List<SavingsGoal> goals = adapter.findByUserId(100L);

        assertThat(goals).hasSize(1);
        assertThat(goals.get(0).userId()).isEqualTo(100L);
        assertThat(goals.get(0).name()).isEqualTo("Meta propia");
    }

    @Test
    void findByUserIdReturnsMultipleGoalsOfTheSameUser() {
        adapter.save(SavingsGoal.open(100L, "Primera", money("1000.00")));
        adapter.save(SavingsGoal.open(100L, "Segunda", money("2000.00")));
        adapter.save(SavingsGoal.open(200L, "De otro", money("3000.00")));

        List<SavingsGoal> goals = adapter.findByUserId(100L);

        assertThat(goals).hasSize(2);
        assertThat(goals).allMatch(goal -> goal.userId().equals(100L));
    }

    @Test
    void findByUserIdReturnsEmptyListWhenUserHasNoGoals() {
        assertThat(adapter.findByUserId(404L)).isEmpty();
    }
}
