package com.quickdeliver.dto.response;

import com.quickdeliver.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private String phone;
    private Role   role;
    private Long   cityId;
    private String cityName;
}
