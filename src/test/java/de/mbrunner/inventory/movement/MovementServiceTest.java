package de.mbrunner.inventory.movement;

import de.mbrunner.inventory.common.BusinessRuleException;
import de.mbrunner.inventory.common.InsufficientStockException;
import de.mbrunner.inventory.common.NotFoundException;
import de.mbrunner.inventory.location.StorageLocation;
import de.mbrunner.inventory.location.StorageLocationRepository;
import de.mbrunner.inventory.movement.MovementDtos.IssueRequest;
import de.mbrunner.inventory.movement.MovementDtos.ReceiptRequest;
import de.mbrunner.inventory.movement.MovementDtos.TransferRequest;
import de.mbrunner.inventory.product.Product;
import de.mbrunner.inventory.product.ProductRepository;
import de.mbrunner.inventory.stock.StockLevel;
import de.mbrunner.inventory.stock.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Business rules of the booking logic, tested without Spring and without a database. */
@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StorageLocationRepository locationRepository;
    @Mock
    private StockLevelRepository stockLevelRepository;
    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private MovementService movementService;

    private Product product;
    private StorageLocation shelfA;
    private StorageLocation shelfB;

    @BeforeEach
    void setUp() {
        product = new Product("SKU-1", "Bolt", "PCS", 10);
        shelfA = new StorageLocation("A-01", "Shelf A1");
        shelfB = new StorageLocation("B-01", "Shelf B1");
    }

    @Test
    void receiptCreatesStockLevelIfLocationNeverHeldTheProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(shelfA));
        when(stockLevelRepository.findForUpdate(any(), any())).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = movementService.receive(new ReceiptRequest(1L, 10L, 25, "PO-4711"));

        assertThat(response.type()).isEqualTo(MovementType.RECEIPT);
        assertThat(response.quantity()).isEqualTo(25);
        assertThat(response.toLocation()).isEqualTo("A-01");
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void issueFromLocationWithoutStockFailsAndBooksNothing() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(shelfA));
        when(stockLevelRepository.findForUpdate(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movementService.issue(new IssueRequest(1L, 10L, 1, null)))
                .isInstanceOf(InsufficientStockException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void issueMoreThanAvailableFails() {
        StockLevel stock = new StockLevel(product, shelfA);
        stock.increase(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(shelfA));
        when(stockLevelRepository.findForUpdate(any(), any())).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> movementService.issue(new IssueRequest(1L, 10L, 6, null)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stock.getQuantity()).isEqualTo(5);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void transferToSameLocationIsRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> movementService.transfer(new TransferRequest(1L, 10L, 10L, 3, null)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productRepository, locationRepository, stockLevelRepository, movementRepository);
    }

    @Test
    void transferMovesQuantityBetweenLocations() {
        StockLevel source = new StockLevel(product, shelfA);
        source.increase(10);
        StockLevel target = new StockLevel(product, shelfB);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(shelfA));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(shelfB));
        // The entities created in this test have no ids, so the lock calls are told apart by call order:
        // the lower location id (10 = source) is locked first.
        when(stockLevelRepository.findForUpdate(any(), any()))
                .thenReturn(Optional.of(source))
                .thenReturn(Optional.of(target));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = movementService.transfer(new TransferRequest(1L, 10L, 20L, 4, "Umlagerung"));

        assertThat(source.getQuantity()).isEqualTo(6);
        assertThat(target.getQuantity()).isEqualTo(4);
        assertThat(response.type()).isEqualTo(MovementType.TRANSFER);
        assertThat(response.fromLocation()).isEqualTo("A-01");
        assertThat(response.toLocation()).isEqualTo("B-01");
    }

    @Test
    void unknownProductResultsInNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movementService.receive(new ReceiptRequest(99L, 10L, 1, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product with id 99");
    }
}
