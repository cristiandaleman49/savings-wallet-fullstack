package com.example.savingswallet.domain.savingsgoal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.example.savingswallet.domain.money.Money;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class SavingsGoalTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Long ID = 1L;
    private static final Long USER_ID = 42L;
    private static final String NAME = "Vacaciones";

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    // --- creation: valid ---

    @Test
    void createsAValidGoalWithItsData() {
        SavingsGoal goal = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("250.50"));

        assertThat(goal.id()).isEqualTo(ID);
        assertThat(goal.userId()).isEqualTo(USER_ID);
        assertThat(goal.name()).isEqualTo(NAME);
        assertThat(goal.targetAmount()).isEqualTo(usd("1000.00"));
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("250.50"));
    }

    // --- creation: id ---

    @Test
    void rejectsNullId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SavingsGoal(null, USER_ID, NAME, usd("1000.00"), usd("0.00")))
                .withMessage("id must not be null");
    }

    // --- creation: userId ---

    @Test
    void rejectsNullUserId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SavingsGoal(ID, null, NAME, usd("1000.00"), usd("0.00")))
                .withMessage("userId must not be null");
    }

    // --- creation: name ---

    @Test
    void rejectsNullName() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, null, usd("1000.00"), usd("0.00")))
                .withMessage("name must not be null");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, "   ", usd("1000.00"), usd("0.00")))
                .withMessage("name must not be blank");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, "", usd("1000.00"), usd("0.00")))
                .withMessage("name must not be blank");
    }

    // --- creation: targetAmount ---

    @Test
    void rejectsNullTargetAmount() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, null, usd("0.00")))
                .withMessage("targetAmount must not be null");
    }

    @Test
    void rejectsZeroTargetAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, usd("0.00"), usd("0.00")))
                .withMessage("targetAmount must be greater than zero");
    }

    @Test
    void rejectsNegativeTargetAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, new Money(new BigDecimal("-100.00"), USD), usd("0.00")))
                .withMessage("amount must not be negative");
    }

    // --- creation: accumulatedAmount ---

    @Test
    void rejectsNullAccumulatedAmount() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), null))
                .withMessage("accumulatedAmount must not be null");
    }

    @Test
    void rejectsNegativeAccumulatedAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), new Money(new BigDecimal("-0.01"), USD)))
                .withMessage("amount must not be negative");
    }

    @Test
    void rejectsAccumulatedAmountGreaterThanTarget() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("1000.01")))
                .withMessage("accumulatedAmount must not exceed targetAmount");
    }

    @Test
    void rejectsAccumulatedAmountInDifferentCurrency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), new Money(BigDecimal.ZERO, EUR)))
                .withMessage("targetAmount and accumulatedAmount must have the same currency");
    }

    // --- status transitions ---

    @Test
    void aNewGoalStartsActive() {
        SavingsGoal goal = SavingsGoal.open(ID, USER_ID, NAME, usd("1000.00"));

        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("0.00"));
    }

    @Test
    void transientGoalHasNoIdUntilPersisted() {
        SavingsGoal goal = SavingsGoal.open(USER_ID, NAME, usd("1000.00"));

        assertThat(goal.id()).isNull();
        assertThat(goal.userId()).isEqualTo(USER_ID);
        assertThat(goal.name()).isEqualTo(NAME);
        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("0.00"));
    }

    @Test
    void goalAlreadyAccumulatedUpToTargetIsCompleted() {
        SavingsGoal goal = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("1000.00"));

        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    // --- contributions ---

    @Test
    void contributionIncreasesAccumulatedAmount() {
        SavingsGoal goal = SavingsGoal.open(ID, USER_ID, NAME, usd("1000.00"));

        goal.contribute(usd("150.00"));

        assertThat(goal.accumulatedAmount()).isEqualTo(usd("150.00"));
        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void contributionReachingTargetCompletesTheGoal() {
        SavingsGoal goal = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("800.00"));

        goal.contribute(usd("200.00"));

        assertThat(goal.accumulatedAmount()).isEqualTo(usd("1000.00"));
        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.COMPLETED);
    }

    @Test
    void completedGoalRejectsFurtherContributions() {
        SavingsGoal goal = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("1000.00"));

        assertThatIllegalStateException()
                .isThrownBy(() -> goal.contribute(usd("1.00")))
                .withMessage("cannot contribute to a COMPLETED goal");
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("1000.00"));
    }

    @Test
    void rejectsContributionThatWouldExceedTarget() {
        SavingsGoal goal = new SavingsGoal(ID, USER_ID, NAME, usd("1000.00"), usd("900.00"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> goal.contribute(usd("100.01")))
                .withMessage("contribution would make accumulatedAmount exceed targetAmount");
        assertThat(goal.accumulatedAmount()).isEqualTo(usd("900.00"));
        assertThat(goal.status()).isEqualTo(SavingsGoalStatus.ACTIVE);
    }

    @Test
    void rejectsNullContribution() {
        SavingsGoal goal = SavingsGoal.open(ID, USER_ID, NAME, usd("1000.00"));

        assertThatNullPointerException()
                .isThrownBy(() -> goal.contribute(null))
                .withMessage("amount must not be null");
    }

    @Test
    void rejectsContributionInADifferentCurrency() {
        SavingsGoal goal = SavingsGoal.open(ID, USER_ID, NAME, usd("1000.00"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> goal.contribute(new Money(new BigDecimal("10.00"), EUR)))
                .withMessage("contribution and targetAmount must have the same currency");
    }
}