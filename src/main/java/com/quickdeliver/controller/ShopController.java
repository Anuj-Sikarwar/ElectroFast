package com.quickdeliver.controller;

import com.quickdeliver.dto.response.ShopResponse;
import com.quickdeliver.enums.ShopStatus;
import com.quickdeliver.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/public")
    public ResponseEntity<Page<ShopResponse>> listPublic(
            @RequestParam Long cityId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(shopService.listByCityAndStatus(cityId, ShopStatus.ACTIVE, pageable));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ShopResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shopService.getById(id));
    }
}
