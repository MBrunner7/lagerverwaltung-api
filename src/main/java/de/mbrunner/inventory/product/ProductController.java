package de.mbrunner.inventory.product;

import de.mbrunner.inventory.product.ProductDtos.CreateProductRequest;
import de.mbrunner.inventory.product.ProductDtos.ProductResponse;
import de.mbrunner.inventory.product.ProductDtos.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product master data")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List products, optionally filtered by SKU or name")
    public Page<ProductResponse> list(@RequestParam(name = "q", required = false) String query,
                                      @PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        return productService.list(query, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update name, unit and reorder point of a product")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }
}
