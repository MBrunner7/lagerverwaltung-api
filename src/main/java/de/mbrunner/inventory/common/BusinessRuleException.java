package de.mbrunner.inventory.common;

/** The request is well-formed but violates a business rule. Mapped to HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
