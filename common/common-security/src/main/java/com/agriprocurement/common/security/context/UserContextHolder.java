package com.agriprocurement.common.security.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ThreadLocal holder for user context information.
 * Extracts and holds user information from JWT claims.
 */
public class UserContextHolder {
    
    private static final Logger logger = LoggerFactory.getLogger(UserContextHolder.class);
    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    private UserContextHolder() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the user context manually.
     */
    public static void setContext(UserContext userContext) {
        contextHolder.set(userContext);
        logger.debug("User context set for user: {}", userContext.username());
    }

    /**
     * Get the current user context.
     * If not set manually, attempts to extract from Spring Security context.
     */
    public static UserContext getContext() {
        UserContext context = contextHolder.get();
        if (context == null) {
            context = extractFromSecurityContext();
            if (context != null) {
                contextHolder.set(context);
            }
        }
        return context;
    }

    /**
     * Clear the user context.
     */
    public static void clearContext() {
        contextHolder.remove();
        logger.debug("User context cleared");
    }

    /**
     * Extract user context from Spring Security context.
     */
    private static UserContext extractFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("No authenticated user found in security context");
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return extractFromJwt(jwtAuth);
        }

        logger.debug("Authentication is not a JWT token: {}", authentication.getClass().getSimpleName());
        return null;
    }

    /**
     * Extract user context from JWT token.
     */
    private static UserContext extractFromJwt(JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();
        
        // Extract user ID (sub claim)
        String userId = jwt.getClaimAsString("sub");
        
        // Extract username (preferred_username or sub)
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("sub");
        }
        
        // Extract email
        String email = jwt.getClaimAsString("email");
        
        // Extract roles from authorities
        Set<String> roles = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove ROLE_ prefix
                .collect(Collectors.toSet());

        logger.debug("Extracted user context - userId: {}, username: {}, roles: {}", 
                userId, username, roles);

        try {
            return new UserContext(userId, username, email, roles);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to create user context from JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the current user ID.
     */
    public static String getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.userId() : null;
    }

    /**
     * Get the current username.
     */
    public static String getCurrentUsername() {
        UserContext context = getContext();
        return context != null ? context.username() : null;
    }

    /**
     * Get the current user email.
     */
    public static String getCurrentUserEmail() {
        UserContext context = getContext();
        return context != null ? context.email() : null;
    }

    /**
     * Get the current user roles.
     */
    public static Set<String> getCurrentUserRoles() {
        UserContext context = getContext();
        return context != null ? context.roles() : Set.of();
    }
}
