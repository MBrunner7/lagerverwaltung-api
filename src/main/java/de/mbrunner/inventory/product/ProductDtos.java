package de.mbrunner.inventory.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, '.', '_' and '-'")
            String sku,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 16) String unit,
            @NotNull @PositiveOrZero Integer minStock) {
    }

    /** The SKU is the business key and cannot be changed. */
    public record UpdateProductRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 16) String unit,
            @NotNull @PositiveOrZero Integer minStock) {
    }

    public record ProductResponse(Long id, String sku, String name, String unit, int minStock, Instant createdAt) {

        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                    product.getUnit(), product.getMinStock(), product.getCreatedAt());
        }
    }
}
