package com.quickdeliver.dto.response;

import com.quickdeliver.entity.Shop;
import com.quickdeliver.enums.ShopStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShopResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String logoUrl;
    private String managerPhone;
    private String managerName;
    private Long cityId;
    private String cityName;
    private ShopStatus status;
    private LocalDateTime createdAt;

    public static ShopResponse from(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .address(shop.getAddress())
                .logoUrl(shop.getLogoUrl())
                .managerPhone(shop.getManagerPhone())
                .managerName(shop.getManagerName())
                .cityId(shop.getCity().getId())
                .cityName(shop.getCity().getName())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .build();
    }
}
