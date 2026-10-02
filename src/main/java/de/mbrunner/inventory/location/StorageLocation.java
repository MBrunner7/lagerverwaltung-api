package de.mbrunner.inventory.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A physical place where goods are stored, e.g. a warehouse bin "A-01-03". */
@Entity
@Table(name = "storage_location")
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    protected StorageLocation() {
        // for JPA
    }

    public StorageLocation(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
