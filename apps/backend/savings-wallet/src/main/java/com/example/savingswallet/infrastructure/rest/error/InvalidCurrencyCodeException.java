package com.example.savingswallet.infrastructure.rest.error;

/**
 * Thrown when a request carries a currency code that is not a real ISO 4217
 * currency. It is a request-shape failure (400), distinct from domain business
 * rule violations (422).
 */
public class InvalidCurrencyCodeException extends RuntimeException {

    public InvalidCurrencyCodeException(String currencyCode) {
        super("unsupported currency: " + currencyCode);
    }
}