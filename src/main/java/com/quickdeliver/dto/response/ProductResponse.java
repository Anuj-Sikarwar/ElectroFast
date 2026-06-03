package com.quickdeliver.dto.response;

import com.quickdeliver.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal mrp;
    private String imageUrl;
    private String brand;
    private String model;
    private int stockQuantity;
    private boolean available;
    private Long shopId;
    private String shopName;
    private String shopAddress;
    // Manager contact shown on product page
    private String shopManagerPhone;
    private String shopManagerName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .mrp(p.getMrp())
                .imageUrl(p.getImageUrl())
                .brand(p.getBrand())
                .model(p.getModel())
                .stockQuantity(p.getStockQuantity())
                .available(p.isAvailable())
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getName())
                .shopAddress(p.getShop().getAddress())
                .shopManagerPhone(p.getShop().getManagerPhone())
                .shopManagerName(p.getShop().getManagerName())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
