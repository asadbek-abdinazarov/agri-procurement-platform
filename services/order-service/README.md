# Order Service

Order management service implementing the Saga pattern for distributed transaction orchestration.

## Features

- **Saga Pattern Orchestration**: Coordinates distributed transactions across inventory and payment services
- **Automatic Compensation**: Rolls back changes when saga steps fail
- **Circuit Breaker**: Protects against cascading failures
- **Retry Mechanism**: Automatically retries failed service calls
- **Service Discovery**: Integrates with Eureka for service registration

## Architecture

### Domain Model
- **Order**: Aggregate root containing order details and saga state
- **OrderItem**: Value object representing items in an order
- **OrderStatus**: PENDING, CONFIRMED, CANCELLED, FAILED
- **SagaStatus**: STARTED, INVENTORY_RESERVED, PAYMENT_PROCESSED, COMPLETED, COMPENSATING, COMPENSATED

### Saga Orchestration Flow

1. **Create Order**: Initialize order with PENDING status and STARTED saga status
2. **Reserve Inventory**: Call inventory service to reserve products
3. **Process Payment**: Call payment service to charge customer
4. **Complete**: Mark order as CONFIRMED with COMPLETED saga status

If any step fails, compensation is triggered:
- Refund payment (if processed)
- Release inventory reservation (if reserved)
- Mark order as FAILED with COMPENSATED saga status

## API Endpoints

### Create Order
```http
POST /api/v1/orders
Content-Type: application/json

{
  "customerId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 10,
      "unitPrice": 100.00
    }
  ]
}
```

### Get Order
```http
GET /api/v1/orders/{orderId}
```

### Get Customer Orders
```http
GET /api/v1/orders/customer/{customerId}
```

## Configuration

### Environment Variables
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: orderdb)
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `EUREKA_SERVER_URL`: Eureka server URL (default: http://localhost:8761/eureka/)

### Service Port
- Default: 8082

## Dependencies

- Spring Boot 3.2.2
- Spring Data JPA
- PostgreSQL
- Flyway for database migrations
- Spring Cloud Eureka Client
- Spring Cloud OpenFeign
- Resilience4j for circuit breaker and retry

## Database Schema

The service uses PostgreSQL with Flyway migrations:
- `orders`: Stores order information and saga state
- `order_items`: Stores line items for each order

## Resilience

### Circuit Breaker
- Configured for inventory-service and payment-service
- Opens after 50% failure rate
- Sliding window of 10 calls
- Half-open after 10 seconds

### Retry
- Maximum 3 attempts
- 1 second wait duration
- Retries on timeout and I/O exceptions

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/order-service-1.0.0-SNAPSHOT.jar

# With custom configuration
java -jar target/order-service-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://dbhost:5432/orderdb \
  --eureka.client.service-url.defaultZone=http://eureka:8761/eureka/
```

## Integration

This service integrates with:
- **Inventory Service**: For product reservation
- **Payment Service**: For payment processing
- **Service Discovery**: For service registration and discovery
- **API Gateway**: For external API access
