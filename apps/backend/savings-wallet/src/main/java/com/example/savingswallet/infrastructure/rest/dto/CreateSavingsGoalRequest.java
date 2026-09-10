package com.example.savingswallet.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Request payload for creating a savings goal.
 *
 * <p>Validates the request shape at the API boundary only. Business rules
 * (target greater than zero, valid currency semantics, etc.) are enforced by the
 * domain via the {@code CreateSavingsGoal} use case.
 */
public record CreateSavingsGoalRequest(

        @NotBlank(message = "name must not be blank")
        String name,

        @NotNull(message = "targetAmount must not be null")
        @DecimalMin(value = "0.01", message = "targetAmount must be greater than zero")
        @Digits(integer = 19, fraction = 2, message = "targetAmount must have at most 2 decimal places")
        BigDecimal targetAmount,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
        String currency) {
}