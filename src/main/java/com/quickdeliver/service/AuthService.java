package com.quickdeliver.service;

import com.quickdeliver.dto.request.LoginRequest;
import com.quickdeliver.dto.request.RegisterRequest;
import com.quickdeliver.dto.response.AuthResponse;
import com.quickdeliver.entity.User;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.repository.UserRepository;
import com.quickdeliver.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final JwtUtil         jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already registered");
        if (userRepository.existsByPhone(request.getPhone()))
            throw new BadRequestException("Phone number already registered");

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isActive())
            throw new BadRequestException("Account is disabled. Contact support.");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BadRequestException("Invalid email or password");

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .cityId(user.getCity()   != null ? user.getCity().getId()   : null)
                .cityName(user.getCity() != null ? user.getCity().getName() : null)
                .build();
    }
}
