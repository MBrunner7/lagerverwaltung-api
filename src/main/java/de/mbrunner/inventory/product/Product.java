package de.mbrunner.inventory.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stock keeping unit: the business key of a product. Immutable after creation. */
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    /** Unit of measure, e.g. PCS, KG, M. */
    @Column(nullable = false, length = 16)
    private String unit;

    /** Reorder point: if the total stock falls below this value the product is reported as low on stock. */
    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
        // for JPA
    }

    public Product(String sku, String name, String unit, int minStock) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.minStock = minStock;
        this.createdAt = Instant.now();
    }

    public void update(String name, String unit, int minStock) {
        this.name = name;
        this.unit = unit;
        this.minStock = minStock;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public int getMinStock() {
        return minStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
