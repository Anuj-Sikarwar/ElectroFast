package com.quickdeliver.config;

import com.quickdeliver.entity.Category;
import com.quickdeliver.entity.City;
import com.quickdeliver.repository.CategoryRepository;
import com.quickdeliver.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds cities and categories on first startup.
 *
 * To make yourself ADMIN after registering:
 *   UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CityRepository     cityRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        seedCities();
        seedCategories();
    }

    private void seedCities() {
        if (cityRepository.count() > 0) return;
        List<City> cities = List.of(
            city("Agra",      "Uttar Pradesh", 27.1767, 78.0081),
            city("Delhi",     "Delhi",         28.6139, 77.2090),
            city("Jaipur",    "Rajasthan",     26.9124, 75.7873),
            city("Lucknow",   "Uttar Pradesh", 26.8467, 80.9462),
            city("Kanpur",    "Uttar Pradesh", 26.4499, 80.3319),
            city("Mathura",   "Uttar Pradesh", 27.4924, 77.6737),
            city("Noida",     "Uttar Pradesh", 28.5355, 77.3910),
            city("Gurgaon",   "Haryana",       28.4595, 77.0266),
            city("Varanasi",  "Uttar Pradesh", 25.3176, 82.9739),
            city("Allahabad", "Uttar Pradesh", 25.4358, 81.8463)
        );
        cityRepository.saveAll(cities);
        log.info("Seeded {} cities", cities.size());
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        List<Category> cats = List.of(
            cat("Headphones & Earphones"),
            cat("Smartphones"),
            cat("Laptops & Computers"),
            cat("Televisions"),
            cat("Refrigerators"),
            cat("Washing Machines"),
            cat("Air Conditioners"),
            cat("Cameras"),
            cat("Tablets"),
            cat("Speakers & Audio"),
            cat("Smartwatches"),
            cat("Gaming"),
            cat("Accessories & Cables"),
            cat("Home Appliances"),
            cat("Power & Charging")
        );
        categoryRepository.saveAll(cats);
        log.info("Seeded {} categories", cats.size());
    }

    private City city(String name, String state, double lat, double lng) {
        City c = new City();
        c.setName(name); c.setState(state);
        c.setLatitude(lat); c.setLongitude(lng);
        return c;
    }

    private Category cat(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }
}
