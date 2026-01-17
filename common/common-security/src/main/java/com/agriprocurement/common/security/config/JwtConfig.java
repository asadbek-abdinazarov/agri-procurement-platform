package com.agriprocurement.common.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JWT token configuration for Keycloak.
 * Configures JWT decoder and custom converters for Keycloak realm roles.
 */
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${keycloak.resource:}")
    private String keycloakResource;

    /**
     * Configure JWT decoder for validating tokens.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder;
        
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (issuerUri != null && !issuerUri.isBlank()) {
            jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        } else {
            throw new IllegalStateException("""
                Either spring.security.oauth2.resourceserver.jwt.jwk-set-uri or \
                spring.security.oauth2.resourceserver.jwt.issuer-uri must be configured
                """);
        }
        
        return jwtDecoder;
    }

    /**
     * Configure Keycloak JWT authentication converter.
     * Maps Keycloak realm_access.roles and resource_access roles to Spring Security authorities.
     */
    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter(keycloakResource);
    }
}
