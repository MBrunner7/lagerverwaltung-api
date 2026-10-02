package de.mbrunner.inventory.stock;

import de.mbrunner.inventory.common.NotFoundException;
import de.mbrunner.inventory.location.StorageLocationRepository;
import de.mbrunner.inventory.product.ProductRepository;
import de.mbrunner.inventory.stock.StockDtos.LowStockResponse;
import de.mbrunner.inventory.stock.StockDtos.StockLevelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read side of the inventory: current stock per product, per location and low-stock alerts. */
@Service
@Transactional(readOnly = true)
public class StockQueryService {

    private final StockLevelRepository stockLevelRepository;
    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;

    public StockQueryService(StockLevelRepository stockLevelRepository,
                             ProductRepository productRepository,
                             StorageLocationRepository locationRepository) {
        this.stockLevelRepository = stockLevelRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
    }

    public List<StockLevelResponse> byProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product", productId);
        }
        return stockLevelRepository.findByProductIdWithDetails(productId).stream()
                .map(StockLevelResponse::from)
                .toList();
    }

    public List<StockLevelResponse> byLocation(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new NotFoundException("Storage location", locationId);
        }
        return stockLevelRepository.findByLocationIdWithDetails(locationId).stream()
                .map(StockLevelResponse::from)
                .toList();
    }

    public List<LowStockResponse> lowStock() {
        return productRepository.findLowStock().stream()
                .map(view -> new LowStockResponse(
                        view.getProductId(),
                        view.getSku(),
                        view.getName(),
                        view.getMinStock(),
                        view.getTotalQuantity(),
                        view.getMinStock() - view.getTotalQuantity()))
                .toList();
    }
}
