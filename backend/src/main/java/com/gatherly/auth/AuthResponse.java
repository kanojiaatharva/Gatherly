package com.gatherly.auth;

public record AuthResponse(
    String token,
    UserDto user
) {}
