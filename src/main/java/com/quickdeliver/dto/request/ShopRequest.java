package com.quickdeliver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShopRequest {

    @NotBlank(message = "Shop name is required")
    private String name;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Manager phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String managerPhone;

    private String managerName;

    private String logoUrl;

    @NotNull(message = "City is required")
    private Long cityId;
}
