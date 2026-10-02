package de.mbrunner.inventory.product;

import de.mbrunner.inventory.common.ConflictException;
import de.mbrunner.inventory.common.NotFoundException;
import de.mbrunner.inventory.product.ProductDtos.CreateProductRequest;
import de.mbrunner.inventory.product.ProductDtos.ProductResponse;
import de.mbrunner.inventory.product.ProductDtos.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(sku)) {
            throw new ConflictException("Product with SKU " + sku + " already exists");
        }
        Product product = new Product(sku, request.name().trim(), request.unit().trim().toUpperCase(),
                request.minStock());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findProduct(id);
        product.update(request.name().trim(), request.unit().trim().toUpperCase(), request.minStock());
        return ProductResponse.from(product);
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    public Page<ProductResponse> list(String query, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(query)
                ? productRepository.search(query.trim(), pageable)
                : productRepository.findAll(pageable);
        return page.map(ProductResponse::from);
    }

    Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }
}
