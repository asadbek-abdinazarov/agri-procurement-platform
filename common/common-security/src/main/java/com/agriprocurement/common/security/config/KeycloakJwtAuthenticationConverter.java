package com.agriprocurement.common.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom JWT converter for Keycloak that extracts roles from:
 * 1. realm_access.roles - Keycloak realm roles
 * 2. resource_access.{client-id}.roles - Keycloak client roles
 * 
 * Maps roles to Spring Security authorities with ROLE_ prefix.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakJwtAuthenticationConverter.class);
    
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final String resourceId;

    public KeycloakJwtAuthenticationConverter() {
        this(null);
    }

    public KeycloakJwtAuthenticationConverter(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimValue = jwt.getClaimAsString(JwtClaimNames.SUB);
        
        logger.debug("Converting JWT for user: {}, authorities: {}", principalClaimValue, authorities);
        
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add default authorities (scope-based)
        Collection<GrantedAuthority> defaultAuthorities = defaultGrantedAuthoritiesConverter.convert(jwt);
        if (defaultAuthorities != null) {
            authorities.addAll(defaultAuthorities);
        }
        
        // Extract realm roles
        Collection<String> realmRoles = extractRealmRoles(jwt);
        authorities.addAll(convertRolesToAuthorities(realmRoles));
        
        // Extract resource/client roles
        Collection<String> resourceRoles = extractResourceRoles(jwt);
        authorities.addAll(convertRolesToAuthorities(resourceRoles));
        
        logger.debug("Extracted {} authorities from JWT", authorities.size());
        
        return authorities;
    }

    private Collection<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            logger.debug("No realm_access claim found in JWT");
            return Set.of();
        }
        
        Object rolesObj = realmAccess.get(ROLES_CLAIM);
        if (rolesObj instanceof List<?> rolesList) {
            Set<String> roles = rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toSet());
            logger.debug("Extracted {} realm roles: {}", roles.size(), roles);
            return roles;
        }
        
        return Set.of();
    }

    private Collection<String> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            logger.debug("No resource_access claim found in JWT");
            return Set.of();
        }
        
        Set<String> allResourceRoles = new HashSet<>();
        
        // If specific resourceId is configured, only extract roles for that client
        if (resourceId != null && !resourceId.isBlank()) {
            Collection<String> clientRoles = extractClientRoles(resourceAccess, resourceId);
            allResourceRoles.addAll(clientRoles);
        } else {
            // Extract roles from all clients
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientId = entry.getKey();
                Collection<String> clientRoles = extractClientRoles(resourceAccess, clientId);
                allResourceRoles.addAll(clientRoles);
            }
        }
        
        logger.debug("Extracted {} resource roles: {}", allResourceRoles.size(), allResourceRoles);
        return allResourceRoles;
    }

    private Collection<String> extractClientRoles(Map<String, Object> resourceAccess, String clientId) {
        Object clientAccessObj = resourceAccess.get(clientId);
        if (!(clientAccessObj instanceof Map<?, ?>)) {
            return Set.of();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> clientAccess = (Map<String, Object>) clientAccessObj;
        Object rolesObj = clientAccess.get(ROLES_CLAIM);
        
        if (rolesObj instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toSet());
        }
        
        return Set.of();
    }

    private Collection<GrantedAuthority> convertRolesToAuthorities(Collection<String> roles) {
        return roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(String::toUpperCase)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
