package com.quickdeliver.controller;

import com.quickdeliver.entity.Category;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<Category>> listActive() {
        return ResponseEntity.ok(categoryRepository.findByActiveTrue());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> addCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody Category updated) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(updated.getName());
        category.setIconUrl(updated.getIconUrl());
        category.setActive(updated.isActive());
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}
