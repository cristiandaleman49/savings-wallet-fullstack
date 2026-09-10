package com.example.savingswallet.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddContributionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Long GOAL_ID = 1L;
    private static final Long USER_ID = 42L;

    @Mock
    private SavingsGoalRepository repository;

    @InjectMocks
    private AddContribution addContribution;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private static SavingsGoal activeGoal(Long goalId, Long userId, String accumulated) {
        return new SavingsGoal(goalId, userId, "Vacaciones", usd("1000.00"), usd(accumulated));
    }

    @Test
    void addsContributionAndReturnsUpdatedGoal() {
        SavingsGoal goal = activeGoal(GOAL_ID, USER_ID, "100.00");
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));
        when(repository.save(goal)).thenAnswer(invocation -> invocation.getArgument(0));

        SavingsGoal result = addContribution.execute(GOAL_ID, USER_ID, usd("50.00"));

        assertThat(result.id()).isEqualTo(GOAL_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.accumulatedAmount()).isEqualTo(usd("150.00"));
        assertThat(result.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void queriesUsingGoalIdAndUserId() {
        SavingsGoal goal = activeGoal(GOAL_ID, USER_ID, "0.00");
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));

        addContribution.execute(GOAL_ID, USER_ID, usd("10.00"));

        verify(repository).findByIdAndUserId(GOAL_ID, USER_ID);
    }

    @Test
    void excludesOtherUsersWhenQuerying() {
        SavingsGoal otherUserGoal = activeGoal(GOAL_ID, 999L, "0.00");
        when(repository.findByIdAndUserId(GOAL_ID, 999L)).thenReturn(Optional.of(otherUserGoal));

        addContribution.execute(GOAL_ID, 999L, usd("10.00"));

        verify(repository).findByIdAndUserId(GOAL_ID, 999L);
    }

    @Test
    void persistsTheUpdatedGoal() {
        SavingsGoal goal = activeGoal(GOAL_ID, USER_ID, "100.00");
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));
        when(repository.save(goal)).thenAnswer(invocation -> invocation.getArgument(0));

        addContribution.execute(GOAL_ID, USER_ID, usd("50.00"));

        ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().accumulatedAmount()).isEqualTo(usd("150.00"));
    }

    @Test
    void goalNotFoundThrowsSavingsGoalNotFoundException() {
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addContribution.execute(GOAL_ID, USER_ID, usd("10.00")))
                .isInstanceOf(SavingsGoalNotFoundException.class)
                .hasMessage("Savings goal with id " + GOAL_ID + " was not found for user " + USER_ID);

        verify(repository, never()).save(any(SavingsGoal.class));
    }

    @Test
    void propagatesRepositoryFailure() {
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID))
                .thenThrow(new IllegalStateException("persistence failed"));

        assertThatThrownBy(() -> addContribution.execute(GOAL_ID, USER_ID, usd("10.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("persistence failed");
    }

    @Test
    void rejectsNullUserId() {
        assertThatNullPointerException()
                .isThrownBy(() -> addContribution.execute(GOAL_ID, null, usd("10.00")))
                .withMessage("userId must not be null");
    }

    @Test
    void rejectsNullGoalId() {
        assertThatNullPointerException()
                .isThrownBy(() -> addContribution.execute(null, USER_ID, usd("10.00")))
                .withMessage("goalId must not be null");
    }

    @Test
    void rejectsNullContribution() {
        assertThatNullPointerException()
                .isThrownBy(() -> addContribution.execute(GOAL_ID, USER_ID, null))
                .withMessage("contribution must not be null");
    }

    @Test
    void rejectsNullRepository() {
        assertThatThrownBy(() -> new AddContribution(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("repository must not be null");
    }

    @Test
    void propagatesDomainRuleViolationWithoutPersisting() {
        SavingsGoal goal = activeGoal(GOAL_ID, USER_ID, "990.00");
        when(repository.findByIdAndUserId(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> addContribution.execute(GOAL_ID, USER_ID, usd("20.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contribution would make accumulatedAmount exceed targetAmount");

        verify(repository, never()).save(any(SavingsGoal.class));
    }
}