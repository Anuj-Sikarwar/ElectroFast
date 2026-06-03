package com.quickdeliver.controller;

import com.quickdeliver.dto.response.ProductResponse;
import com.quickdeliver.service.ProductService;
import com.quickdeliver.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private InputSanitizer sanitizer;

    // Browse all products in a city
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {

        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(productService.searchInCity(cityId, sanitizer.sanitize(q), pageable));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(productService.listByCityAndCategory(cityId, categoryId, pageable));
        }
        return ResponseEntity.ok(productService.listByCity(cityId, pageable));
    }

    // Product detail (includes shop manager phone)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }
}
