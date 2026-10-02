package de.mbrunner.inventory.stock;

import de.mbrunner.inventory.common.InsufficientStockException;
import de.mbrunner.inventory.location.StorageLocation;
import de.mbrunner.inventory.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockLevelTest {

    private StockLevel stock;

    @BeforeEach
    void setUp() {
        stock = new StockLevel(new Product("SKU-1", "Bolt", "PCS", 10), new StorageLocation("A-01", "Shelf A1"));
    }

    @Test
    void newStockLevelStartsAtZero() {
        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    void increaseAndDecreaseChangeQuantity() {
        stock.increase(10);
        stock.decrease(4);

        assertThat(stock.getQuantity()).isEqualTo(6);
    }

    @Test
    void decreaseToExactlyZeroIsAllowed() {
        stock.increase(5);
        stock.decrease(5);

        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    void decreaseBelowZeroIsRejectedAndLeavesQuantityUnchanged() {
        stock.increase(3);

        assertThatThrownBy(() -> stock.decrease(4))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 4, available 3");
        assertThat(stock.getQuantity()).isEqualTo(3);
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        assertThatThrownBy(() -> stock.increase(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.decrease(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
