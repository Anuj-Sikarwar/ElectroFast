package com.quickdeliver.dto.response;

import com.quickdeliver.entity.Order;
import com.quickdeliver.enums.OrderStatus;
import com.quickdeliver.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private String deliveryPhone;
    private String notes;
    private List<ItemLine> items;
    private LocalDateTime createdAt;

    // Delivery guy info
    private Long   assignedToId;
    private String assignedToName;
    private String assignedToEmail;

    @Data
    @Builder
    public static class ItemLine {
        private Long productId;
        private String productName;
        private String productImage;
        private String shopName;
        private String shopAddress;
        private String shopManagerPhone;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    public static OrderResponse from(Order order) {
        List<ItemLine> lines = Collections.emptyList();

        if (order.getItems() != null) {
            lines = order.getItems().stream().map(i -> {
                String productName  = i.getProduct() != null ? i.getProduct().getName() : "Unknown";
                String productImage = i.getProduct() != null ? i.getProduct().getImageUrl() : null;
                String shopName     = (i.getProduct() != null && i.getProduct().getShop() != null) ? i.getProduct().getShop().getName() : "Unknown";
                String shopAddress  = (i.getProduct() != null && i.getProduct().getShop() != null) ? i.getProduct().getShop().getAddress() : null;
                String managerPhone = (i.getProduct() != null && i.getProduct().getShop() != null) ? i.getProduct().getShop().getManagerPhone() : null;

                return ItemLine.builder()
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(productName)
                        .productImage(productImage)
                        .shopName(shopName)
                        .shopAddress(shopAddress)
                        .shopManagerPhone(managerPhone)
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build();
            }).toList();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryPhone(order.getDeliveryPhone())
                .notes(order.getNotes())
                .items(lines)
                .createdAt(order.getCreatedAt())
                .assignedToId(order.getAssignedTo() != null ? order.getAssignedTo().getId() : null)
                .assignedToName(order.getAssignedTo() != null ? order.getAssignedTo().getName() : null)
                .assignedToEmail(order.getAssignedTo() != null ? order.getAssignedTo().getEmail() : null)
                .build();
    }
}
