package de.mbrunner.inventory.stock;

public final class StockDtos {

    private StockDtos() {
    }

    public record StockLevelResponse(
            Long productId,
            String sku,
            String productName,
            Long locationId,
            String locationCode,
            int quantity) {

        static StockLevelResponse from(StockLevel level) {
            return new StockLevelResponse(
                    level.getProduct().getId(),
                    level.getProduct().getSku(),
                    level.getProduct().getName(),
                    level.getLocation().getId(),
                    level.getLocation().getCode(),
                    level.getQuantity());
        }
    }

    /** A product whose total stock is below its reorder point, with the quantity missing to reach it. */
    public record LowStockResponse(
            Long productId,
            String sku,
            String name,
            int minStock,
            long totalQuantity,
            long shortfall) {
    }
}
