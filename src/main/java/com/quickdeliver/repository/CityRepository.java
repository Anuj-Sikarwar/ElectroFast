package com.quickdeliver.repository;

import com.quickdeliver.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameIgnoreCase(String name);

    List<City> findByActiveTrue();

    /**
     * Haversine-formula query — finds all active cities within radiusKm
     * of the given coordinates, ordered by distance ascending.
     * Returns the closest city first so the caller can pick [0].
     */
    @Query(value = """
        SELECT *, (
            6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                * COS(RADIANS(longitude) - RADIANS(:lng))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
            )
        ) AS distance
        FROM cities
        WHERE active = true
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<City> findNearestWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );
}
