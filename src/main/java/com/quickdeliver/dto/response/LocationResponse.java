package com.quickdeliver.dto.response;

import com.quickdeliver.entity.City;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Returned by POST /location/resolve
 *
 * matched = true  → cityId + cityName are populated; frontend loads products.
 * matched = false → detectedCityName tells user where we think they are,
 *                   availableCities lets them pick the nearest served city.
 */
@Data
@Builder
public class LocationResponse {

    private boolean matched;

    // Populated when matched = true
    private Long cityId;
    private String cityName;
    private String cityState;

    // Populated when matched = false
    private String detectedCityName;          // what Nominatim resolved
    private List<City> availableCities;       // all served cities for manual pick

    private String message;

    public static LocationResponse matched(City city) {
        return LocationResponse.builder()
                .matched(true)
                .cityId(city.getId())
                .cityName(city.getName())
                .cityState(city.getState())
                .message("Delivering to " + city.getName())
                .build();
    }

    public static LocationResponse noMatch(String detectedName, List<City> cities) {
        String msg = detectedName != null
                ? "We don't deliver to " + detectedName + " yet. Please select a city."
                : "Your location is outside our delivery zones. Please select a city.";

        return LocationResponse.builder()
                .matched(false)
                .detectedCityName(detectedName)
                .availableCities(cities)
                .message(msg)
                .build();
    }
}
