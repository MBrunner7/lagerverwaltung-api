package de.mbrunner.inventory.location;

import de.mbrunner.inventory.location.LocationDtos.CreateLocationRequest;
import de.mbrunner.inventory.location.LocationDtos.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Storage locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @Operation(summary = "Create a storage location")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody CreateLocationRequest request) {
        LocationResponse created = locationService.create(request);
        return ResponseEntity.created(URI.create("/api/locations/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List all storage locations")
    public List<LocationResponse> list() {
        return locationService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a storage location by id")
    public LocationResponse get(@PathVariable Long id) {
        return locationService.get(id);
    }
}
