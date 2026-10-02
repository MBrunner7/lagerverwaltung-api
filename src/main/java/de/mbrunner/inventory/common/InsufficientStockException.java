package de.mbrunner.inventory.common;

/** A movement would take more stock out of a location than is available. Mapped to HTTP 409. */
public class InsufficientStockException extends RuntimeException {

    private final int available;
    private final int requested;

    public InsufficientStockException(int available, int requested) {
        super("Insufficient stock: requested " + requested + ", available " + available);
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
