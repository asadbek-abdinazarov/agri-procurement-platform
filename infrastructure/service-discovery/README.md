# Service Discovery

Netflix Eureka Server implementation for service discovery in the Agricultural Procurement Platform.

## Overview

This module provides centralized service discovery and registration for all microservices in the platform. Services can dynamically discover and communicate with each other without hardcoded endpoints.

## Features

- **Service Registration**: Automatic registration of microservices
- **Service Discovery**: Dynamic lookup of service instances
- **Health Checking**: Monitors service health with heartbeat mechanism
- **Load Balancing**: Provides service instance information for client-side load balancing
- **Self-Preservation**: Protects against network partition scenarios
- **Metrics & Monitoring**: Prometheus metrics and health endpoints

## Quick Start

### Prerequisites

- JDK 17 or higher
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Run

#### Standalone Mode (Single Node)

```bash
mvn spring-boot:run
```

Or using the JAR:

```bash
java -jar target/service-discovery-1.0.0-SNAPSHOT.jar
```

#### High Availability Mode (Multiple Nodes)

For production environments, run multiple Eureka servers:

**Node 1:**
```bash
java -jar target/service-discovery-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=ha \
  --server.port=8761 \
  --eureka.instance.hostname=eureka-peer-1
```

**Node 2:**
```bash
java -jar target/service-discovery-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=ha \
  --server.port=8762 \
  --eureka.instance.hostname=eureka-peer-2
```

## Configuration

### Default Settings

- **Port**: 8761 (Eureka default)
- **Mode**: Standalone (self-registration disabled)
- **Self-Preservation**: Enabled (85% threshold)
- **Lease Renewal**: 30 seconds
- **Lease Expiration**: 90 seconds

### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8761 | Eureka server port |
| `eureka.client.register-with-eureka` | false | Self-registration (false for standalone) |
| `eureka.server.enable-self-preservation` | true | Prevents mass deregistration |
| `eureka.instance.lease-renewal-interval-in-seconds` | 30 | Heartbeat interval |
| `eureka.instance.lease-expiration-duration-in-seconds` | 90 | Time before deregistration |

### Environment Variables

You can override configuration using environment variables:

```bash
export SERVER_PORT=8761
export EUREKA_INSTANCE_HOSTNAME=localhost
java -jar target/service-discovery-1.0.0-SNAPSHOT.jar
```

## Endpoints

### Eureka Dashboard

- **URL**: http://localhost:8761
- **Description**: Web UI showing registered services and their status

### Actuator Endpoints

- **Health**: http://localhost:8761/actuator/health
- **Metrics**: http://localhost:8761/actuator/metrics
- **Prometheus**: http://localhost:8761/actuator/prometheus
- **Info**: http://localhost:8761/actuator/info

### Service Registration

Services register at: `http://localhost:8761/eureka/apps/{SERVICE_NAME}`

## Client Configuration

To register a service with Eureka, add to your microservice:

### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Application Configuration

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
```

## Monitoring

### Health Check

```bash
curl http://localhost:8761/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

### Metrics

View Prometheus metrics:
```bash
curl http://localhost:8761/actuator/prometheus
```

### Check Registered Services

```bash
curl http://localhost:8761/eureka/apps
```

## Docker Deployment

### Dockerfile Example

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/service-discovery-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Run

```bash
docker build -t service-discovery:latest .
docker run -p 8761:8761 service-discovery:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  eureka-server:
    image: service-discovery:latest
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=standalone
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

## High Availability Setup

For production, deploy multiple Eureka servers and configure them to replicate:

### Configuration for HA

Update `application.yml` with the `ha` profile or set environment variables:

```yaml
spring:
  profiles: ha

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-1:8761/eureka/,http://eureka-2:8762/eureka/
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: eureka-server
spec:
  serviceName: eureka-server
  replicas: 3
  selector:
    matchLabels:
      app: eureka-server
  template:
    metadata:
      labels:
        app: eureka-server
    spec:
      containers:
      - name: eureka-server
        image: service-discovery:latest
        ports:
        - containerPort: 8761
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "ha"
```

## Troubleshooting

### Services Not Registering

1. Check network connectivity to Eureka server
2. Verify `eureka.client.service-url.defaultZone` in client configuration
3. Check client service logs for connection errors

### Self-Preservation Mode Triggered

This happens when < 85% of services send heartbeats. It's normal during:
- Network issues
- Mass service restarts
- Development/testing

To disable (not recommended for production):
```yaml
eureka:
  server:
    enable-self-preservation: false
```

### High Memory Usage

Adjust cache settings:
```yaml
eureka:
  server:
    response-cache-auto-expiration-in-seconds: 60
    response-cache-update-interval-ms: 15000
```

## Best Practices

1. **Use Multiple Eureka Instances**: Deploy at least 2-3 instances in production
2. **Configure Proper Timeouts**: Balance between fast failure detection and network flakiness
3. **Enable Self-Preservation**: Protects against false positives during network partitions
4. **Monitor Metrics**: Track registration/deregistration rates and heartbeat failures
5. **Secure Endpoints**: Use Spring Security for production deployments
6. **Resource Planning**: Allocate sufficient memory (2-4GB recommended for production)

## Security Considerations

For production deployments, consider:

1. **Enable Authentication**: Add Spring Security to protect Eureka endpoints
2. **Use HTTPS**: Configure SSL/TLS for Eureka server
3. **Network Isolation**: Deploy in private network/VPC
4. **Access Control**: Restrict Eureka dashboard access

## Performance Tuning

### JVM Options

```bash
java -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar target/service-discovery-1.0.0-SNAPSHOT.jar
```

### Cache Optimization

For high-traffic environments:
```yaml
eureka:
  server:
    response-cache-update-interval-ms: 15000
    use-read-only-response-cache: true
```

## Support

For issues or questions:
- Check logs in `logs/service-discovery.log`
- Review Eureka dashboard at http://localhost:8761
- Monitor health endpoints
- Check service registration status

## License

Copyright © 2024 Agricultural Procurement Platform
