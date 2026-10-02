package de.mbrunner.inventory.movement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class MovementDtos {

    private MovementDtos() {
    }

    /** Goods receipt into a location. */
    public record ReceiptRequest(
            @NotNull Long productId,
            @NotNull Long locationId,
            @NotNull @Positive Integer quantity,
            @Size(max = 100) String reference) {
    }

    /** Goods issue out of a location. */
    public record IssueRequest(
            @NotNull Long productId,
            @NotNull Long locationId,
            @NotNull @Positive Integer quantity,
            @Size(max = 100) String reference) {
    }

    /** Move stock from one location to another. */
    public record TransferRequest(
            @NotNull Long productId,
            @NotNull Long fromLocationId,
            @NotNull Long toLocationId,
            @NotNull @Positive Integer quantity,
            @Size(max = 100) String reference) {
    }

    public record MovementResponse(
            Long id,
            MovementType type,
            Long productId,
            String sku,
            String fromLocation,
            String toLocation,
            int quantity,
            String reference,
            Instant createdAt) {

        static MovementResponse from(StockMovement movement) {
            return new MovementResponse(
                    movement.getId(),
                    movement.getType(),
                    movement.getProduct().getId(),
                    movement.getProduct().getSku(),
                    movement.getFromLocation() == null ? null : movement.getFromLocation().getCode(),
                    movement.getToLocation() == null ? null : movement.getToLocation().getCode(),
                    movement.getQuantity(),
                    movement.getReference(),
                    movement.getCreatedAt());
        }
    }
}
