# Procurement Service

The Procurement Service manages agricultural procurement opportunities and vendor bidding processes in the Agricultural Procurement Platform.

## Architecture

This service follows Domain-Driven Design (DDD) principles with clean architecture layers:

### Domain Layer (`domain/`)
- **Procurement** - Aggregate root managing procurement lifecycle
- **Bid** - Entity representing vendor bids
- **ProcurementRepository** - Repository interface for data access

### Application Layer (`application/`)
- **ProcurementService** - Core business logic orchestration
- **ProcurementCacheService** - Redis caching implementation
- DTOs: `CreateProcurementRequest`, `SubmitBidRequest`, `ProcurementResponse`
- **ProcurementMapper** - MapStruct mapper for entity-DTO conversion

### API Layer (`api/`)
- **ProcurementController** - REST endpoints
- **ApiResponse** - Standard response wrapper
- **GlobalExceptionHandler** - Centralized exception handling

### Infrastructure Layer (`infrastructure/`)
- **CacheConfiguration** - Redis cache configuration
- **ProcurementScheduledTasks** - Scheduled tasks for automated processes

## Features

### Procurement Management
- Create procurement opportunities with budget and deadline
- Publish and open bidding
- Close bidding automatically at deadline
- Award procurement to winning bid
- Cancel procurement before awarding

### Bid Management
- Submit bids from vendors
- Prevent duplicate bids from same vendor
- Validate bid amounts against budget
- Track bid status (submitted, accepted, rejected)

### Resilience
- Circuit breaker pattern with Resilience4j
- Retry mechanism for transient failures
- Bulkhead for request isolation
- Fallback methods for graceful degradation

### Caching
- Redis-based caching for procurement data
- Automatic cache eviction on updates
- Configurable TTL (30 minutes default)

### Event-Driven
- Publishes domain events to Kafka
  - `ProcurementCreatedEvent`
  - `BidSubmittedEvent`
- Event metadata and versioning

## API Endpoints

### Create Procurement
```http
POST /api/procurements
Content-Type: application/json

{
  "title": "Agricultural Equipment Procurement",
  "description": "Procurement for farming equipment",
  "quantityAmount": 100,
  "quantityUnit": "PIECE",
  "budgetAmount": 50000,
  "budgetCurrency": "USD",
  "deadline": "2024-02-15T23:59:59",
  "buyerId": "uuid"
}
```

### Submit Bid
```http
POST /api/procurements/{id}/bids
Content-Type: application/json

{
  "vendorId": "uuid",
  "bidAmount": 45000,
  "bidCurrency": "USD",
  "notes": "Competitive bid with quality guarantee"
}
```

### Award Procurement
```http
PUT /api/procurements/{id}/award?bidId={bidId}
```

### Get Procurement
```http
GET /api/procurements/{id}
```

### List Procurements
```http
GET /api/procurements
GET /api/procurements?activeOnly=true
GET /api/procurements?buyerId={buyerId}
```

### Publish Procurement
```http
PUT /api/procurements/{id}/publish
```

### Close Bidding
```http
PUT /api/procurements/{id}/close-bidding
```

### Cancel Procurement
```http
PUT /api/procurements/{id}/cancel
```

## Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/procurement_db
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

resilience4j:
  circuitbreaker:
    instances:
      procurementService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

## Database Schema

### Procurements Table
- `id` (UUID, PK)
- `title` (VARCHAR)
- `description` (VARCHAR)
- `quantity_amount` (NUMERIC)
- `quantity_unit` (VARCHAR)
- `budget_amount` (NUMERIC)
- `budget_currency` (VARCHAR)
- `deadline` (TIMESTAMP)
- `status` (VARCHAR)
- `buyer_id` (UUID)
- `awarded_bid_id` (UUID, nullable)
- `created_at`, `updated_at`, `version`

### Bids Table
- `id` (UUID, PK)
- `procurement_id` (UUID, FK)
- `vendor_id` (UUID)
- `bid_amount` (NUMERIC)
- `bid_currency` (VARCHAR)
- `bid_date` (TIMESTAMP)
- `status` (VARCHAR)
- `notes` (VARCHAR)
- `created_at`, `updated_at`, `version`

## Running the Service

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis 6+
- Kafka 3.5+

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Run with Docker Compose
```bash
docker-compose up -d
```

## Testing

### Unit Tests
```bash
mvn test -Dtest=ProcurementDomainTest
```

### Integration Tests
Uses Testcontainers for PostgreSQL and Kafka:
```bash
mvn test -Dtest=ProcurementIntegrationTest
```

### All Tests
```bash
mvn verify
```

## Monitoring

Health check endpoint:
```http
GET /actuator/health
```

Metrics:
```http
GET /actuator/metrics
GET /actuator/prometheus
```

## Business Rules

1. **Procurement Lifecycle**
   - Draft → Published → Bidding Open → Bidding Closed → Awarded
   - Can be cancelled at any time before awarding

2. **Bidding Rules**
   - Bids only accepted when status is BIDDING_OPEN
   - Bid amount cannot exceed budget
   - One bid per vendor per procurement
   - Bidding closes automatically at deadline

3. **Award Rules**
   - Can only award after bidding is closed
   - Only submitted bids can be awarded
   - All other bids are automatically rejected

## Domain Events

Published to Kafka topics:

- **procurement.created** - When procurement is created
- **bid.submitted** - When a vendor submits a bid
- **procurement.awarded** - When procurement is awarded (future)
- **procurement.cancelled** - When procurement is cancelled (future)

## Dependencies

- Spring Boot 3.2.2
- Spring Cloud 2023.0.0
- PostgreSQL Driver
- Spring Data JPA
- Spring Data Redis
- Spring Kafka
- Resilience4j 2.2.0
- MapStruct 1.5.5
- Flyway
- Common Modules (domain, events, security, observability)

## Future Enhancements

- [ ] Multi-round bidding support
- [ ] Bid evaluation criteria beyond price
- [ ] Automated bid ranking
- [ ] Email notifications for bid events
- [ ] Document attachments for procurements
- [ ] Vendor qualification checks
- [ ] Reverse auction support
