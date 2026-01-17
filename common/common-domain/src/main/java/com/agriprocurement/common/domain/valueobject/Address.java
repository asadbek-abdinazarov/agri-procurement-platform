package com.agriprocurement.common.domain.valueobject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Immutable value object representing a physical address.
 */
public record Address(
    @NotBlank String street,
    @NotBlank String city,
    @NotBlank String region,
    @NotBlank String country,
    @NotNull String postalCode
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be null or blank");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region cannot be null or blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be null or blank");
        }
        if (postalCode == null) {
            throw new IllegalArgumentException("Postal code cannot be null");
        }
    }

    public String getFullAddress() {
        return String.format("%s, %s, %s, %s %s", street, city, region, country, postalCode);
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
