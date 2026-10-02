package de.mbrunner.inventory.location;

import de.mbrunner.inventory.common.ConflictException;
import de.mbrunner.inventory.common.NotFoundException;
import de.mbrunner.inventory.location.LocationDtos.CreateLocationRequest;
import de.mbrunner.inventory.location.LocationDtos.LocationResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final StorageLocationRepository locationRepository;

    public LocationService(StorageLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LocationResponse create(CreateLocationRequest request) {
        String code = request.code().trim().toUpperCase();
        if (locationRepository.existsByCode(code)) {
            throw new ConflictException("Storage location with code " + code + " already exists");
        }
        StorageLocation location = locationRepository.save(new StorageLocation(code, request.name().trim()));
        return LocationResponse.from(location);
    }

    public LocationResponse get(Long id) {
        return locationRepository.findById(id)
                .map(LocationResponse::from)
                .orElseThrow(() -> new NotFoundException("Storage location", id));
    }

    public List<LocationResponse> list() {
        return locationRepository.findAll(Sort.by("code")).stream()
                .map(LocationResponse::from)
                .toList();
    }
}
