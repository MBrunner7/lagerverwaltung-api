package de.mbrunner.inventory.movement;

import de.mbrunner.inventory.location.StorageLocation;
import de.mbrunner.inventory.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Immutable journal entry for a goods movement. Movements are never updated or deleted,
 * so the history of every stock level can be reconstructed.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private MovementType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id", updatable = false)
    private StorageLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id", updatable = false)
    private StorageLocation toLocation;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(updatable = false, length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
        // for JPA
    }

    private StockMovement(MovementType type, Product product, StorageLocation fromLocation,
                          StorageLocation toLocation, int quantity, String reference) {
        this.type = type;
        this.product = product;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.quantity = quantity;
        this.reference = reference;
        this.createdAt = Instant.now();
    }

    public static StockMovement receipt(Product product, StorageLocation to, int quantity, String reference) {
        return new StockMovement(MovementType.RECEIPT, product, null, to, quantity, reference);
    }

    public static StockMovement issue(Product product, StorageLocation from, int quantity, String reference) {
        return new StockMovement(MovementType.ISSUE, product, from, null, quantity, reference);
    }

    public static StockMovement transfer(Product product, StorageLocation from, StorageLocation to,
                                         int quantity, String reference) {
        return new StockMovement(MovementType.TRANSFER, product, from, to, quantity, reference);
    }

    public Long getId() {
        return id;
    }

    public MovementType getType() {
        return type;
    }

    public Product getProduct() {
        return product;
    }

    public StorageLocation getFromLocation() {
        return fromLocation;
    }

    public StorageLocation getToLocation() {
        return toLocation;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
