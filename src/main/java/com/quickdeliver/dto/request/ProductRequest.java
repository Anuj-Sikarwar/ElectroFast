package com.quickdeliver.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal mrp;

    private String imageUrl;
    private String brand;
    private String model;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stockQuantity;

    private Long categoryId;
}
