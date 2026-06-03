package com.quickdeliver.service;

import com.quickdeliver.dto.response.LocationResponse;
import com.quickdeliver.entity.City;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Location resolution flow:
 *
 * 1. Frontend gets browser geolocation (lat, lng) — asks user permission.
 * 2. Frontend sends coords to POST /location/resolve
 * 3. Backend checks DB for nearest active city within radius (Haversine SQL).
 * 4. If found → return city. Products then load filtered by that cityId.
 * 5. If not found → return list of all active cities so user can pick manually.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final CityRepository cityRepository;
    private final GeocodingService geocodingService;

    @Value("${app.location.radius.km:50}")
    private double radiusKm;

    /**
     * Tries to match coordinates to a served city.
     * Returns a LocationResponse indicating whether a match was found,
     * the matched city (if any), and fallback city list if not.
     */
    public LocationResponse resolveLocation(double latitude, double longitude) {
        // Step 1: Haversine DB query — find nearest city within radius
        Optional<City> nearest = cityRepository.findNearestWithinRadius(latitude, longitude, radiusKm);

        if (nearest.isPresent()) {
            City city = nearest.get();
            log.info("Coords ({},{}) matched city: {}", latitude, longitude, city.getName());
            return LocationResponse.matched(city);
        }

        // Step 2: No DB match — try reverse geocoding to tell user why
        String resolvedName = geocodingService.reverseGeocode(latitude, longitude);
        log.info("Coords ({},{}) resolved to '{}' but no city match within {}km",
                latitude, longitude, resolvedName, radiusKm);

        // Step 3: Return all active cities for manual selection
        List<City> allCities = cityRepository.findByActiveTrue();
        return LocationResponse.noMatch(resolvedName, allCities);
    }

    /**
     * Validates that a given cityId is active and served.
     * Used when user picks city manually from the list.
     */
    public LocationResponse selectCity(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new BadRequestException("City not found: " + cityId));

        if (!city.isActive()) {
            throw new BadRequestException("We don't deliver to " + city.getName() + " yet.");
        }

        return LocationResponse.matched(city);
    }
}
