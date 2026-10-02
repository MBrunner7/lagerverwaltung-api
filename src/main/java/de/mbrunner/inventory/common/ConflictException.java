package de.mbrunner.inventory.common;

/** The request conflicts with the current state, e.g. a duplicate business key. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
