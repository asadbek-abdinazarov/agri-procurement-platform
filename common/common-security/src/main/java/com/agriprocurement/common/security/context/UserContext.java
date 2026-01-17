package com.agriprocurement.common.security.context;

import java.util.Set;

/**
 * Record to hold current user information extracted from JWT token.
 */
public record UserContext(
    String userId,
    String username,
    String email,
    Set<String> roles
) {
    public UserContext {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be null or blank");
        }
        roles = roles != null ? Set.copyOf(roles) : Set.of();
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
