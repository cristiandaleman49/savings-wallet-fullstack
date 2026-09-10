package com.example.savingswallet.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void createsMoneyWithZeroOrPositiveAmount() {
        Money money = new Money(new BigDecimal("10.50"), USD);

        assertThat(money.amount()).isEqualByComparingTo("10.50");
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void allowsZeroAmount() {
        Money money = new Money(BigDecimal.ZERO, USD);

        assertThat(money.amount()).isZero();
    }

    @Test
    void rejectsNullAmount() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Money(null, USD))
                .withMessage("amount must not be null");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Money(BigDecimal.ONE, null))
                .withMessage("currency must not be null");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Money(new BigDecimal("-0.01"), USD))
                .withMessage("amount must not be negative");
    }

    @Test
    void isEqualWhenAmountAndCurrencyAreEqual() {
        Money first = new Money(new BigDecimal("10.50"), USD);
        Money second = new Money(new BigDecimal("10.50"), USD);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    void isEqualWhenAmountsDifferOnlyByScale() {
        Money first = new Money(new BigDecimal("10.50"), USD);
        Money second = new Money(new BigDecimal("10.500"), USD);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    void isNotEqualWhenAmountOrCurrencyDiffers() {
        Money money = new Money(new BigDecimal("10.50"), USD);

        assertThat(money)
                .isNotEqualTo(new Money(new BigDecimal("20.00"), USD))
                .isNotEqualTo(new Money(new BigDecimal("10.50"), Currency.getInstance("EUR")));
    }
}
