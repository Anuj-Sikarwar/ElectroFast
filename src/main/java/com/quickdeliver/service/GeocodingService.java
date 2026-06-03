package com.quickdeliver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reverse-geocodes (lat, lng) → city/town name using
 * OpenStreetMap Nominatim API (free, no API key needed).
 *
 * Nominatim ToS: max 1 request/second, must set a User-Agent.
 * For production scale, swap with Google Maps Geocoding API
 * or cache results in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    @Value("${geocoding.nominatim.url}")
    private String nominatimUrl;

    @Value("${geocoding.nominatim.user-agent}")
    private String userAgent;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Returns the city/town name for the given coordinates.
     * Tries city → town → county → state_district in that priority.
     * Returns null if no result could be resolved.
     */
    public String reverseGeocode(double latitude, double longitude) {
        try {
            WebClient client = webClientBuilder
                    .baseUrl(nominatimUrl)
                    .defaultHeader("User-Agent", userAgent)
                    .build();

            String json = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("zoom", 10)        // city-level zoom
                            .queryParam("addressdetails", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isBlank()) return null;

            JsonNode root = objectMapper.readTree(json);
            JsonNode address = root.path("address");

            // Priority: city > town > village > county > state_district
            for (String field : new String[]{"city", "town", "village", "county", "state_district"}) {
                JsonNode node = address.path(field);
                if (!node.isMissingNode() && !node.asText().isBlank()) {
                    String resolved = node.asText().trim();
                    log.info("Reverse geocoded ({}, {}) → {}", latitude, longitude, resolved);
                    return resolved;
                }
            }

        } catch (Exception e) {
            log.error("Reverse geocoding failed for ({}, {}): {}", latitude, longitude, e.getMessage());
        }

        return null;
    }
}
