# Common Security Module

OAuth2/OIDC Resource Server security configuration for Keycloak integration.

## Overview

This module provides reusable Spring Security configuration for all microservices in the Agricultural Procurement Platform. It implements OAuth2 Resource Server with JWT authentication using Keycloak as the identity provider.

## Features

- **OAuth2 Resource Server**: JWT-based authentication for microservices
- **Keycloak Integration**: Custom JWT converter for Keycloak realm and client roles
- **User Context Management**: ThreadLocal holder for user information from JWT claims
- **Method Security**: Enabled `@PreAuthorize`, `@Secured`, and JSR-250 annotations
- **Public Endpoints**: Configurable patterns for health checks and Swagger documentation

## Configuration

### Application Properties

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/agri-procurement
          # OR use jwk-set-uri directly:
          # jwk-set-uri: http://keycloak:8080/realms/agri-procurement/protocol/openid-connect/certs

# Optional: Specify Keycloak client ID for resource access roles
keycloak:
  resource: agri-procurement-client
```

### Maven Dependency

Add this dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.agriprocurement</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

## Components

### SecurityConfig

Main security configuration that sets up:
- JWT authentication via OAuth2 Resource Server
- Public endpoints (actuator, Swagger)
- Stateless session management
- Method-level security

### JwtConfig

Configures:
- JWT decoder for token validation
- Custom Keycloak authentication converter

### KeycloakJwtAuthenticationConverter

Extracts and converts Keycloak roles to Spring Security authorities:
- **Realm roles**: From `realm_access.roles`
- **Client roles**: From `resource_access.{client-id}.roles`
- Automatically adds `ROLE_` prefix to all roles

### UserContext

Immutable record holding user information:
- `userId` (sub claim)
- `username` (preferred_username)
- `email`
- `roles` (Set of role names)

### UserContextHolder

ThreadLocal holder for accessing current user context:

```java
// Get current user
UserContext user = UserContextHolder.getContext();
String userId = UserContextHolder.getCurrentUserId();
String username = UserContextHolder.getCurrentUsername();
Set<String> roles = UserContextHolder.getCurrentUserRoles();

// Check roles
if (user.hasRole("ADMIN")) {
    // Admin-only logic
}
```

## Usage Examples

### Method Security

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Order> getAllOrders() {
        String userId = UserContextHolder.getCurrentUserId();
        return orderService.findByUserId(userId);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(@PathVariable String id) {
        orderService.delete(id);
    }
}
```

### Accessing User Context

```java
@Service
public class OrderService {
    
    public Order createOrder(OrderRequest request) {
        UserContext user = UserContextHolder.getContext();
        
        Order order = new Order();
        order.setUserId(user.userId());
        order.setUserEmail(user.email());
        order.setCreatedBy(user.username());
        
        return orderRepository.save(order);
    }
}
```

## Public Endpoints

The following endpoints are public by default:
- `/actuator/health/**`
- `/actuator/info`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/error`

To customize public endpoints, extend `SecurityConfig` and override the `securityFilterChain` method.

## JWT Token Format

Expected JWT claims:
- `sub`: User ID (required)
- `preferred_username`: Username (optional, falls back to `sub`)
- `email`: User email address (optional)
- `realm_access.roles`: Array of realm roles
- `resource_access.{client-id}.roles`: Array of client-specific roles

Example JWT payload:
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "preferred_username": "john.doe",
  "email": "john.doe@example.com",
  "realm_access": {
    "roles": ["USER", "FARMER"]
  },
  "resource_access": {
    "agri-procurement-client": {
      "roles": ["ORDER_MANAGER"]
    }
  }
}
```

This will be converted to Spring Security authorities:
- `ROLE_USER`
- `ROLE_FARMER`
- `ROLE_ORDER_MANAGER`

## Dependencies

- Spring Boot 3.2.2
- Spring Security 6.2.1
- Spring Security OAuth2 Resource Server
- Spring Security OAuth2 JOSE (JWT support)

## Notes

- Uses Spring Security 6.x conventions (Lambda DSL)
- Stateless session management (no server-side sessions)
- Automatic role mapping with `ROLE_` prefix
- Thread-safe user context management
- Compatible with all Spring Boot 3.x microservices
