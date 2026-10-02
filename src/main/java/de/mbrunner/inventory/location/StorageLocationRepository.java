package de.mbrunner.inventory.location;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    boolean existsByCode(String code);
}
