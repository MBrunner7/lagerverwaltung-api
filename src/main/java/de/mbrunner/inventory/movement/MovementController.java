package de.mbrunner.inventory.movement;

import de.mbrunner.inventory.movement.MovementDtos.IssueRequest;
import de.mbrunner.inventory.movement.MovementDtos.MovementResponse;
import de.mbrunner.inventory.movement.MovementDtos.ReceiptRequest;
import de.mbrunner.inventory.movement.MovementDtos.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
@Tag(name = "Stock movements", description = "Goods receipts, goods issues and transfers")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a goods receipt into a storage location")
    public MovementResponse receive(@Valid @RequestBody ReceiptRequest request) {
        return movementService.receive(request);
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a goods issue out of a storage location (fails with 409 if stock is insufficient)")
    public MovementResponse issue(@Valid @RequestBody IssueRequest request) {
        return movementService.issue(request);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer stock between two storage locations")
    public MovementResponse transfer(@Valid @RequestBody TransferRequest request) {
        return movementService.transfer(request);
    }

    @GetMapping
    @Operation(summary = "Movement history of a product, newest first")
    public Page<MovementResponse> history(@RequestParam Long productId,
                                          @PageableDefault(size = 50) Pageable pageable) {
        return movementService.history(productId, pageable);
    }
}
