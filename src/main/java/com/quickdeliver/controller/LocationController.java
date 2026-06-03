package com.quickdeliver.controller;

import com.quickdeliver.dto.request.LocationRequest;
import com.quickdeliver.dto.response.LocationResponse;
import com.quickdeliver.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Location resolution endpoints — all public (no auth needed).
 *
 * Frontend flow:
 * 1. Browser asks user for geolocation permission.
 * 2. On grant  → POST /location/resolve with {latitude, longitude}
 * 3. On deny   → GET  /location/cities  to show manual picker
 * 4. On manual → POST /location/select/{cityId}
 */
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * Resolves browser coordinates to a served city using
     * Haversine distance query + Nominatim reverse geocoding fallback.
     */
    @PostMapping("/resolve")
    public ResponseEntity<LocationResponse> resolve(
            @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(
                locationService.resolveLocation(request.getLatitude(), request.getLongitude())
        );
    }

    /**
     * User manually selects a city from the list.
     */
    @PostMapping("/select/{cityId}")
    public ResponseEntity<LocationResponse> select(@PathVariable Long cityId) {
        return ResponseEntity.ok(locationService.selectCity(cityId));
    }
}
