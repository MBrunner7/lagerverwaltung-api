package de.mbrunner.inventory.stock;

import de.mbrunner.inventory.stock.StockDtos.LowStockResponse;
import de.mbrunner.inventory.stock.StockDtos.StockLevelResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "Current stock levels")
public class StockController {

    private final StockQueryService stockQueryService;

    public StockController(StockQueryService stockQueryService) {
        this.stockQueryService = stockQueryService;
    }

    @GetMapping
    @Operation(summary = "Stock levels of one product or of one location (exactly one parameter is required)")
    public List<StockLevelResponse> stock(@RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long locationId) {
        if ((productId == null) == (locationId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of the parameters 'productId' or 'locationId'");
        }
        return productId != null
                ? stockQueryService.byProduct(productId)
                : stockQueryService.byLocation(locationId);
    }

    @GetMapping("/low")
    @Operation(summary = "Products whose total stock is below their reorder point")
    public List<LowStockResponse> lowStock() {
        return stockQueryService.lowStock();
    }
}
