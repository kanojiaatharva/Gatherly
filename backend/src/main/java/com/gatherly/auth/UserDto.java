package com.gatherly.auth;

import java.time.Instant;

public record UserDto(
    Long id,
    String email,
    String displayName,
    Instant createdAt
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getCreatedAt()
        );
    }
}
