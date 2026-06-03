package com.quickdeliver.dto.request;

import com.quickdeliver.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    @NotBlank(message = "Delivery phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String deliveryPhone;

    private String notes;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
    }
}
