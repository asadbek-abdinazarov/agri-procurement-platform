# Common Observability Module

Production-ready observability module providing metrics, distributed tracing, and structured logging for the Agricultural Procurement Platform.

## Features

### 1. Metrics
- **Prometheus Integration**: Exposes metrics in Prometheus format at `/actuator/prometheus`
- **Business Metrics**: Domain-specific metrics for procurement, orders, and inventory
- **Common Tags**: Automatic tagging with application, environment, region, and version
- **Custom Metrics Support**: Easy-to-use API for recording custom metrics

### 2. Distributed Tracing
- **Micrometer Tracing**: Distributed tracing with Brave
- **Zipkin Integration**: Automatic span reporting to Zipkin
- **Trace Correlation**: Trace ID and Span ID included in logs via MDC

### 3. Structured Logging
- **JSON Logging**: Structured logs in JSON format for production
- **Text Logging**: Human-readable logs for development
- **Profile-based Configuration**: Different log levels per environment
- **Trace Context**: Automatic inclusion of trace ID and span ID in logs

## Usage

### Add Dependency

Add to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.agriprocurement</groupId>
    <artifactId>common-observability</artifactId>
</dependency>
```

### Configuration

The module is auto-configured. Override properties in `application.properties`:

```properties
# Application identification
spring.application.name=procurement-service

# Environment configuration
observability.environment=production
observability.region=us-east-1
observability.version=1.0.0

# Tracing configuration
management.tracing.sampling.probability=0.1
management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans

# Metrics configuration
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

### Business Metrics

Inject `BusinessMetrics` to record domain-specific metrics:

```java
@Service
public class ProcurementService {
    
    private final BusinessMetrics metrics;
    
    public ProcurementService(BusinessMetrics metrics) {
        this.metrics = metrics;
    }
    
    public void createProcurement(ProcurementRequest request) {
        Timer.Sample sample = metrics.startProcurementTimer();
        try {
            // Business logic here
            metrics.recordProcurementCreated("agricultural-equipment");
        } finally {
            metrics.stopProcurementTimer(sample);
        }
    }
    
    public void awardProcurement(String id) {
        metrics.recordProcurementAwarded();
    }
}
```

### Actuator Endpoints

Available endpoints:

- `/actuator/health` - Health check
- `/actuator/info` - Application information
- `/actuator/metrics` - Available metrics
- `/actuator/metrics/{metricName}` - Specific metric details
- `/actuator/prometheus` - Prometheus metrics endpoint

### Logging

Use SLF4J for logging. Trace context is automatically added:

```java
@Slf4j
@Service
public class OrderService {
    
    public void processOrder(OrderRequest order) {
        log.info("Processing order: {}", order.getId());
        // Logs will include: [traceId,spanId]
    }
}
```

## Available Business Metrics

### Procurement Metrics
- `agri.procurement.created` - Total procurements created (Counter)
- `agri.procurement.awarded` - Total procurements awarded (Counter)
- `agri.procurement.cancelled` - Total procurements cancelled (Counter)
- `agri.procurement.processing.duration` - Processing time (Timer)
- `agri.procurement.active` - Active procurements count (Gauge)

### Order Metrics
- `agri.order.created` - Total orders created (Counter)
- `agri.order.completed` - Total orders completed (Counter)
- `agri.order.failed` - Total orders failed (Counter)
- `agri.order.processing.duration` - Processing time (Timer)
- `agri.order.active` - Active orders count (Gauge)

### Inventory Metrics
- `agri.inventory.reserved` - Total items reserved (Counter)
- `agri.inventory.released` - Total items released (Counter)
- `agri.inventory.check.duration` - Check time (Timer)
- `agri.inventory.reserved.items` - Current reserved items (Gauge)

## Metric Naming Conventions

All metrics follow these conventions:
- Prefix: `agri.` for agricultural platform
- Snake case: `procurement.created` instead of `procurementCreated`
- Unit suffixes: `.duration` for timers, `.items` for counts
- Common tags: All metrics include `application`, `environment`, `region`, `version`
- Optional tags: Category, type, reason for detailed filtering

## Log Patterns

### Development (Plain Text)
```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO  c.a.p.ProcurementController - [abc123,def456] - Processing procurement request
```

### Production (JSON)
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.agriprocurement.procurement.ProcurementController",
  "message": "Processing procurement request",
  "thread": "http-nio-8080-exec-1",
  "application": "procurement-service",
  "environment": "production",
  "traceId": "abc123",
  "spanId": "def456"
}
```

## Profiles

- `dev`, `local` - DEBUG logs, text format, console only
- `staging`, `stg` - INFO logs, JSON format, console + file
- `prod`, `production` - INFO logs, JSON format, console + file, optimized

## Integration with Monitoring

### Prometheus
Configure Prometheus to scrape:
```yaml
scrape_configs:
  - job_name: 'agri-platform'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['service:8080']
```

### Grafana Dashboards
Import dashboards to visualize:
- JVM metrics (heap, GC, threads)
- HTTP metrics (requests, latency, errors)
- Business metrics (procurements, orders, inventory)

### Zipkin/Jaeger
Set endpoint to collect traces:
```properties
management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
```

## Best Practices

1. **Use Timers**: Wrap operations with timer samples to measure duration
2. **Tag Metrics**: Add meaningful tags for filtering and aggregation
3. **Sample Traces**: Adjust sampling rate based on traffic (0.1 = 10%)
4. **Structured Logging**: Use structured arguments for better searchability
5. **MDC Context**: Add user ID or correlation ID to MDC for request tracing

## Dependencies

- Spring Boot Actuator 3.2.2
- Micrometer (Prometheus, Tracing)
- Zipkin Reporter
- Logback with Logstash Encoder

## License

Proprietary - Agricultural Procurement Platform
