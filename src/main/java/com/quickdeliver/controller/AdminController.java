package com.quickdeliver.controller;

import com.quickdeliver.dto.request.ProductRequest;
import com.quickdeliver.dto.request.ShopRequest;
import com.quickdeliver.dto.response.OrderResponse;
import com.quickdeliver.dto.response.ProductResponse;
import com.quickdeliver.dto.response.ShopResponse;
import com.quickdeliver.enums.OrderStatus;
import com.quickdeliver.enums.ShopStatus;
import com.quickdeliver.service.DeliveryService;
import com.quickdeliver.service.OrderService;
import com.quickdeliver.service.ProductService;
import com.quickdeliver.service.ShopService;
import com.quickdeliver.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopService    shopService;
    private final ProductService productService;
    private final OrderService   orderService;
    private final DeliveryService deliveryService;

    // ─── Shop management ────────────────────────────────────────────────────

    @PostMapping("/shops")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<ShopResponse> registerShop(
            @Valid @RequestBody ShopRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(shopService.register(request, authentication.getName()));
    }

    @PutMapping("/shops/{shopId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable Long shopId,
            @Valid @RequestBody ShopRequest request) {
        return ResponseEntity.ok(shopService.update(shopId, request));
    }

    @PutMapping("/shops/{shopId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> setShopStatus(
            @PathVariable Long shopId,
            @RequestParam ShopStatus status) {
        shopService.setStatus(shopId, status);
        return ResponseEntity.ok(Map.of("message", "Shop status updated to " + status));
    }

    @GetMapping("/shops")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<Page<ShopResponse>> listAllShops(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(shopService.listAll(pageable));
    }

    @GetMapping("/shops/{shopId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<ShopResponse> getShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.getById(shopId));
    }

    // ─── Product management ─────────────────────────────────────────────────

    @PostMapping("/shops/{shopId}/products")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<ProductResponse> addProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.add(shopId, request));
    }

    @PutMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long productId) {
        productService.delete(productId);
        return ResponseEntity.ok(Map.of("message", "Product deactivated"));
    }

    @PutMapping("/products/{productId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<Map<String, String>> updateStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        productService.updateStock(productId, quantity);
        return ResponseEntity.ok(Map.of("message", "Stock updated to " + quantity));
    }

    // ─── Order management ───────────────────────────────────────────────────

    /**
     * List all orders with optional filters:
     * ?status=PENDING&cityId=1&date=2025-01-15&page=0&size=20
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrdersFiltered(status, cityId, date, pageable));
    }

    // ─── Delivery management ────────────────────────────────────────────────

    @GetMapping("/delivery/personnel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getDeliveryPersonnel() {
        return ResponseEntity.ok(deliveryService.getAllDeliveryPersonnel());
    }

    @PutMapping("/orders/{orderId}/assign/{deliveryUserId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<OrderResponse> assignDelivery(
            @PathVariable Long orderId,
            @PathVariable Long deliveryUserId) {
        return ResponseEntity.ok(deliveryService.assignDelivery(orderId, deliveryUserId));
    }

    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<Map<String, String>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        orderService.updateStatus(orderId, status);
        return ResponseEntity.ok(Map.of("message", "Order status updated to " + status));
    }
}
