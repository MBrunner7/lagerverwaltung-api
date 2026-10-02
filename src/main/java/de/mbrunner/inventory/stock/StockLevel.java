package de.mbrunner.inventory.stock;

import de.mbrunner.inventory.common.InsufficientStockException;
import de.mbrunner.inventory.location.StorageLocation;
import de.mbrunner.inventory.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Current quantity of one product at one storage location.
 * <p>
 * The quantity is only changed through {@link #increase(int)} and {@link #decrease(int)},
 * so the rule "stock never goes negative" lives in one place.
 */
@Entity
@Table(name = "stock_level")
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, updatable = false)
    private StorageLocation location;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private long version;

    protected StockLevel() {
        // for JPA
    }

    public StockLevel(Product product, StorageLocation location) {
        this.product = product;
        this.location = location;
        this.quantity = 0;
    }

    public void increase(int amount) {
        requirePositive(amount);
        this.quantity = Math.addExact(this.quantity, amount);
    }

    public void decrease(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new InsufficientStockException(quantity, amount);
        }
        this.quantity -= amount;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public StorageLocation getLocation() {
        return location;
    }

    public int getQuantity() {
        return quantity;
    }
}
