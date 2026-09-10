package com.example.savingswallet.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetSavingsGoalsTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Long USER_ID = 42L;

    @Mock
    private SavingsGoalRepository repository;

    @InjectMocks
    private GetSavingsGoals getSavingsGoals;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private static SavingsGoal goal(Long id, Long userId, String name, String target) {
        return new SavingsGoal(id, userId, name, usd(target), usd("0.00"));
    }

    @Test
    void returnsTheGoalsReturnedByTheRepository() {
        SavingsGoal first = goal(1L, USER_ID, "Vacaciones", "1000.00");
        SavingsGoal second = goal(2L, USER_ID, "Fondo", "2000.00");
        when(repository.findByUserId(USER_ID)).thenReturn(List.of(first, second));

        List<SavingsGoal> result = getSavingsGoals.execute(USER_ID);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void delegatesUsingTheCorrectUserId() {
        getSavingsGoals.execute(USER_ID);

        verify(repository).findByUserId(USER_ID);
    }

    @Test
    void returnsEmptyListWhenRepositoryHasNoGoals() {
        when(repository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThat(getSavingsGoals.execute(USER_ID)).isEmpty();
    }

    @Test
    void propagatesRepositoryFailure() {
        when(repository.findByUserId(USER_ID)).thenThrow(new IllegalStateException("query failed"));

        assertThatThrownBy(() -> getSavingsGoals.execute(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("query failed");
    }

    @Test
    void rejectsNullUserId() {
        assertThatNullPointerException()
                .isThrownBy(() -> getSavingsGoals.execute(null))
                .withMessage("userId must not be null");
    }

    @Test
    void rejectsNullRepository() {
        assertThatThrownBy(() -> new GetSavingsGoals(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("repository must not be null");
    }
}