package com.portfolio.Library.Management.infra.security;

public record AuthResponse(String token, String tokenType, Long userId,
                           String name, String email, String role) {
}
