package de.mbrunner.inventory.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(value = """
            select m from StockMovement m
            join fetch m.product
            left join fetch m.fromLocation
            left join fetch m.toLocation
            where m.product.id = :productId
            order by m.createdAt desc, m.id desc
            """,
            countQuery = "select count(m) from StockMovement m where m.product.id = :productId")
    Page<StockMovement> findByProductId(@Param("productId") Long productId, Pageable pageable);
}
