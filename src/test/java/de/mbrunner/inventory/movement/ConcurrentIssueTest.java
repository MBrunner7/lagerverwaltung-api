package de.mbrunner.inventory.movement;

import de.mbrunner.inventory.TestcontainersConfiguration;
import de.mbrunner.inventory.common.InsufficientStockException;
import de.mbrunner.inventory.location.LocationDtos.CreateLocationRequest;
import de.mbrunner.inventory.location.LocationService;
import de.mbrunner.inventory.movement.MovementDtos.IssueRequest;
import de.mbrunner.inventory.movement.MovementDtos.ReceiptRequest;
import de.mbrunner.inventory.product.ProductDtos.CreateProductRequest;
import de.mbrunner.inventory.product.ProductService;
import de.mbrunner.inventory.stock.StockQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the row lock in {@code StockLevelRepository.findForUpdate} prevents overselling:
 * 8 parallel goods issues of 1 unit compete for 5 units in stock. Exactly 5 must succeed
 * and the stock must end at 0, never below.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context configuration as the API test, so both share one database container
@Import(TestcontainersConfiguration.class)
class ConcurrentIssueTest {

    private static final int STOCK = 5;
    private static final int PARALLEL_REQUESTS = 8;

    @Autowired
    private ProductService productService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private MovementService movementService;
    @Autowired
    private StockQueryService stockQueryService;

    @Test
    void parallelIssuesNeverDriveStockBelowZero() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long productId = productService.create(new CreateProductRequest("RACE-" + suffix, "Race test", "PCS", 0)).id();
        Long locationId = locationService.create(new CreateLocationRequest("RACE-" + suffix, "Race shelf")).id();
        movementService.receive(new ReceiptRequest(productId, locationId, STOCK, null));

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_REQUESTS)) {
            for (int i = 0; i < PARALLEL_REQUESTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        movementService.issue(new IssueRequest(productId, locationId, 1, null));
                        return true;
                    } catch (InsufficientStockException e) {
                        return false;
                    }
                }));
            }
            start.countDown(); // release all threads at the same moment

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(STOCK);
        }

        assertThat(stockQueryService.byProduct(productId))
                .singleElement()
                .satisfies(level -> assertThat(level.quantity()).isZero());
    }
}
