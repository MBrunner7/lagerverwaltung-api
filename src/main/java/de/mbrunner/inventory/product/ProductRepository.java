package de.mbrunner.inventory.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    @Query("""
            select p from Product p
            where lower(p.sku) like lower(concat('%', :query, '%'))
               or lower(p.name) like lower(concat('%', :query, '%'))
            """)
    Page<Product> search(@Param("query") String query, Pageable pageable);

    /** Products whose total stock across all locations is below their reorder point. */
    @Query("""
            select p.id as productId, p.sku as sku, p.name as name, p.minStock as minStock,
                   coalesce(sum(s.quantity), 0) as totalQuantity
            from Product p
            left join StockLevel s on s.product = p
            group by p.id, p.sku, p.name, p.minStock
            having coalesce(sum(s.quantity), 0) < p.minStock
            order by p.sku
            """)
    List<LowStockView> findLowStock();

    interface LowStockView {
        Long getProductId();

        String getSku();

        String getName();

        Integer getMinStock();

        Long getTotalQuantity();
    }
}
