package de.mbrunner.inventory.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class LocationDtos {

    private LocationDtos() {
    }

    public record CreateLocationRequest(
            @NotBlank @Size(max = 32)
            @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, '.', '_' and '-'")
            String code,
            @NotBlank @Size(max = 200) String name) {
    }

    public record LocationResponse(Long id, String code, String name) {

        static LocationResponse from(StorageLocation location) {
            return new LocationResponse(location.getId(), location.getCode(), location.getName());
        }
    }
}
