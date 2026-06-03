package com.quickdeliver.controller;

import com.quickdeliver.dto.request.OrderRequest;
import com.quickdeliver.dto.response.OrderResponse;
import com.quickdeliver.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            Principal principal) {
        return ResponseEntity.ok(orderService.placeOrder(principal.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> myOrders(
            Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(principal.getName(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }
}
