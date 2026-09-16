package com.diversao.backend.user;

public record AuthResponse(
        String token,
        String refreshToken,
        String email,
        String name,
        String role
) {}