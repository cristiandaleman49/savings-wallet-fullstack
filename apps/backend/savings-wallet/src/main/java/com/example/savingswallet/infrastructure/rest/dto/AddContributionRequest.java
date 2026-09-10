package com.example.savingswallet.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Request payload for contributing to an existing savings goal.
 *
 * <p>Validates the request shape at the API boundary only. Contribution rules
 * (positive amount, target not exceeded, completed goal rejection) are enforced
 * by {@code SavingsGoal.contribute} via the {@code AddContribution} use case.
 */
public record AddContributionRequest(

        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(integer = 19, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
        String currency) {
}