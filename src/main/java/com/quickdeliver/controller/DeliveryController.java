package com.quickdeliver.controller;

import com.quickdeliver.dto.response.OrderResponse;
import com.quickdeliver.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // All unassigned confirmed orders — delivery guy picks one
    @GetMapping("/available")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        return ResponseEntity.ok(deliveryService.getAvailableOrders());
    }

    // Accept an order — assigns himself
    @PostMapping("/accept/{orderId}")
    public ResponseEntity<OrderResponse> acceptOrder(
            Principal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.acceptOrder(principal.getName(), orderId));
    }

    // My assigned deliveries
    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> myDeliveries(
            Principal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(principal.getName(), pageable));
    }

    // Mark out for delivery
    @PutMapping("/{orderId}/out-for-delivery")
    public ResponseEntity<OrderResponse> markOutForDelivery(
            Principal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.markOutForDelivery(principal.getName(), orderId));
    }

    // Mark delivered (also marks COD payment collected)
    @PutMapping("/{orderId}/delivered")
    public ResponseEntity<OrderResponse> markDelivered(
            Principal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.markDelivered(principal.getName(), orderId));
    }
}
