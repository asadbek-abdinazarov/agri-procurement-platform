# API Gateway

API Gateway for the Agricultural Procurement Platform using Spring Cloud Gateway.

## Features

- **Service Discovery**: Integrates with Eureka for dynamic service routing
- **Rate Limiting**: Redis-backed rate limiting (100 requests/minute per user)
- **Circuit Breaker**: Resilience4j circuit breaker pattern for fault tolerance
- **Retry Mechanism**: Exponential backoff retry for transient failures
- **Request Logging**: Comprehensive request/response logging with correlation IDs
- **Fallback Responses**: Graceful degradation with fallback endpoints
- **Observability**: Metrics, health checks, and distributed tracing

## Routes

The gateway routes requests to the following services:

- `/api/procurement/**` → Procurement Service
- `/api/orders/**` → Order Service
- `/api/inventory/**` → Inventory Service
- `/api/payments/**` → Payment Service
- `/api/logistics/**` → Logistics Service
- `/api/notifications/**` → Notification Service
- `/api/users/**` → User Service
- `/api/analytics/**` → Analytics Service

## Configuration

### Environment Variables

- `EUREKA_SERVER_URL`: Eureka server URL (default: http://localhost:8761/eureka/)
- `REDIS_HOST`: Redis host for rate limiting (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

### Rate Limiting

Rate limiting is configured per user with the following defaults:
- Replenish rate: 100 requests per minute
- Burst capacity: 200 requests
- Uses Redis for distributed rate limiting

### Circuit Breaker

Each service has its own circuit breaker with:
- Sliding window size: 20 requests
- Failure rate threshold: 50%
- Wait duration in open state: 30 seconds
- Timeout: 10 seconds

### Retry Policy

- Maximum attempts: 3
- Initial backoff: 100ms
- Maximum backoff: 1000ms
- Exponential backoff factor: 2
- Only retries on GET requests

## Endpoints

### Health Check
```
GET /actuator/health
```

### Metrics
```
GET /actuator/metrics
GET /actuator/prometheus
```

### Gateway Routes
```
GET /actuator/gateway/routes
```

### Circuit Breaker Status
```
GET /actuator/circuitbreakers
```

## Running Locally

```bash
mvn spring-boot:run
```

The gateway will start on port 8080.

## Dependencies

- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- Spring Data Redis Reactive
- Resilience4j
- Common Security Module
- Common Observability Module

## Architecture

The API Gateway acts as the single entry point for all client requests, providing:

1. **Routing**: Dynamic routing based on service discovery
2. **Security**: Integration with common security module for authentication
3. **Resilience**: Circuit breakers and retries for fault tolerance
4. **Rate Limiting**: Protection against excessive requests
5. **Observability**: Logging, metrics, and tracing for all requests

## Monitoring

The gateway exposes various actuator endpoints for monitoring:

- Health checks for circuit breakers and rate limiters
- Prometheus metrics for request rates, latencies, and errors
- Gateway route information and configuration

## Error Handling

When a service is unavailable, the circuit breaker opens and routes requests to fallback endpoints that return appropriate error messages with HTTP 503 status.
