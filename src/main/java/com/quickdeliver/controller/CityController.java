package com.quickdeliver.controller;

import com.quickdeliver.entity.City;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityRepository cityRepository;

    @GetMapping
    public ResponseEntity<List<City>> listActiveCities() {
        return ResponseEntity.ok(cityRepository.findAll()
                .stream()
                .filter(City::isActive)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<City> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<City> addCity(@RequestBody City city) {
        return ResponseEntity.ok(cityRepository.save(city));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<City> toggleCity(@PathVariable Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + id));
        city.setActive(!city.isActive());
        return ResponseEntity.ok(cityRepository.save(city));
    }
}
