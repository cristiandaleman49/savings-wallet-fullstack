package com.example.savingswallet.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.savingswallet.application.port.out.SavingsGoalRepository;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateSavingsGoalTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Long ID = 1L;
    private static final Long USER_ID = 42L;
    private static final String NAME = "Vacaciones";

    @Mock
    private SavingsGoalRepository repository;

    @InjectMocks
    private CreateSavingsGoal createSavingsGoal;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    @Test
    void createsSavingsGoalWithExpectedData() {
        when(repository.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createSavingsGoal.execute(ID, USER_ID, NAME, usd("1000.00"));

        ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(repository).save(captor.capture());
        SavingsGoal goal = captor.getValue();
        assertThat(goal.id()).isEqualTo(ID);
        assertThat(goal.userId()).isEqualTo(USER_ID);
        assertThat(goal.name()).isEqualTo(NAME);
        assertThat(goal.targetAmount()).isEqualTo(usd("1000.00"));
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("0.00"));
        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void delegatesPersistenceToSavingsGoalRepository() {
        createSavingsGoal.execute(ID, USER_ID, NAME, usd("1000.00"));

        verify(repository).save(any(SavingsGoal.class));
    }

    @Test
    void returnsTheRepositoryResult() {
        SavingsGoal persisted = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("0.00"));
        when(repository.save(any(SavingsGoal.class))).thenReturn(persisted);

        SavingsGoal result = createSavingsGoal.execute(ID, USER_ID, NAME, usd("1000.00"));

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void rejectsInvalidDomainDataWithoutTouchingTheRepository() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createSavingsGoal.execute(ID, USER_ID, "   ", usd("1000.00")))
                .withMessage("name must not be blank");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> createSavingsGoal.execute(ID, USER_ID, NAME, usd("0.00")))
                .withMessage("targetAmount must be greater than zero");

        verifyNoInteractions(repository);
    }

    @Test
    void propagatesRepositoryFailure() {
        when(repository.save(any(SavingsGoal.class))).thenThrow(new IllegalStateException("persistence failed"));

        assertThatThrownBy(() -> createSavingsGoal.execute(ID, USER_ID, NAME, usd("1000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("persistence failed");
    }

    @Test
    void rejectsNullRepository() {
        assertThatThrownBy(() -> new CreateSavingsGoal(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("repository must not be null");
    }
}
