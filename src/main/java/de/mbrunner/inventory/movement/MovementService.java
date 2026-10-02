package de.mbrunner.inventory.movement;

import de.mbrunner.inventory.common.BusinessRuleException;
import de.mbrunner.inventory.common.InsufficientStockException;
import de.mbrunner.inventory.common.NotFoundException;
import de.mbrunner.inventory.location.StorageLocation;
import de.mbrunner.inventory.location.StorageLocationRepository;
import de.mbrunner.inventory.movement.MovementDtos.IssueRequest;
import de.mbrunner.inventory.movement.MovementDtos.MovementResponse;
import de.mbrunner.inventory.movement.MovementDtos.ReceiptRequest;
import de.mbrunner.inventory.movement.MovementDtos.TransferRequest;
import de.mbrunner.inventory.product.Product;
import de.mbrunner.inventory.product.ProductRepository;
import de.mbrunner.inventory.stock.StockLevel;
import de.mbrunner.inventory.stock.StockLevelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books goods movements. Every booking runs in one transaction that
 * <ol>
 *     <li>locks the affected stock levels,</li>
 *     <li>changes the quantities (the entity enforces "never negative") and</li>
 *     <li>writes an immutable journal entry.</li>
 * </ol>
 * Either all of it is persisted or nothing is.
 */
@Service
@Transactional
public class MovementService {

    private final ProductRepository productRepository;
    private final StorageLocationRepository locationRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;

    public MovementService(ProductRepository productRepository,
                           StorageLocationRepository locationRepository,
                           StockLevelRepository stockLevelRepository,
                           StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.movementRepository = movementRepository;
    }

    public MovementResponse receive(ReceiptRequest request) {
        Product product = findProduct(request.productId());
        StorageLocation location = findLocation(request.locationId());

        lockOrCreate(product, location).increase(request.quantity());

        StockMovement movement = StockMovement.receipt(product, location, request.quantity(), request.reference());
        return MovementResponse.from(movementRepository.save(movement));
    }

    public MovementResponse issue(IssueRequest request) {
        Product product = findProduct(request.productId());
        StorageLocation location = findLocation(request.locationId());

        lockExisting(product, location, request.quantity()).decrease(request.quantity());

        StockMovement movement = StockMovement.issue(product, location, request.quantity(), request.reference());
        return MovementResponse.from(movementRepository.save(movement));
    }

    public MovementResponse transfer(TransferRequest request) {
        if (request.fromLocationId().equals(request.toLocationId())) {
            throw new BusinessRuleException("Source and target location must be different");
        }
        Product product = findProduct(request.productId());
        StorageLocation from = findLocation(request.fromLocationId());
        StorageLocation to = findLocation(request.toLocationId());

        // Always lock the two rows in the same order (lower location id first). Otherwise a transfer
        // A->B running in parallel with a transfer B->A could each hold one lock and wait for the other: a deadlock.
        StockLevel source;
        StockLevel target;
        if (request.fromLocationId() < request.toLocationId()) {
            source = lockExisting(product, from, request.quantity());
            target = lockOrCreate(product, to);
        } else {
            target = lockOrCreate(product, to);
            source = lockExisting(product, from, request.quantity());
        }

        source.decrease(request.quantity());
        target.increase(request.quantity());

        StockMovement movement = StockMovement.transfer(product, from, to, request.quantity(), request.reference());
        return MovementResponse.from(movementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> history(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product", productId);
        }
        return movementRepository.findByProductId(productId, pageable).map(MovementResponse::from);
    }

    private StockLevel lockOrCreate(Product product, StorageLocation location) {
        return stockLevelRepository.findForUpdate(product.getId(), location.getId())
                .orElseGet(() -> stockLevelRepository.save(new StockLevel(product, location)));
    }

    /** A location that never held the product has no stock row, which simply means zero stock. */
    private StockLevel lockExisting(Product product, StorageLocation location, int requested) {
        return stockLevelRepository.findForUpdate(product.getId(), location.getId())
                .orElseThrow(() -> new InsufficientStockException(0, requested));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    private StorageLocation findLocation(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new NotFoundException("Storage location", id));
    }
}
