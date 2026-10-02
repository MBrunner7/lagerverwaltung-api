package de.mbrunner.inventory.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    /**
     * Loads a stock level with a row lock (SELECT ... FOR UPDATE). Concurrent movements on the
     * same product and location are serialized, so two parallel goods issues cannot both
     * see the same quantity and drive the stock below zero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockLevel s where s.product.id = :productId and s.location.id = :locationId")
    Optional<StockLevel> findForUpdate(@Param("productId") Long productId, @Param("locationId") Long locationId);

    @Query("""
            select s from StockLevel s
            join fetch s.product
            join fetch s.location l
            where s.product.id = :productId
            order by l.code
            """)
    List<StockLevel> findByProductIdWithDetails(@Param("productId") Long productId);

    @Query("""
            select s from StockLevel s
            join fetch s.product p
            join fetch s.location
            where s.location.id = :locationId
            order by p.sku
            """)
    List<StockLevel> findByLocationIdWithDetails(@Param("locationId") Long locationId);
}
