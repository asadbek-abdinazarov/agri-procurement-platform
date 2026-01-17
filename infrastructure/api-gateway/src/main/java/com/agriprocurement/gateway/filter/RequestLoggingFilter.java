package com.agriprocurement.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String correlationId = getOrCreateCorrelationId(request);
        exchange.getAttributes().put(REQUEST_START_TIME, System.currentTimeMillis());
        
        try {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
            
            String userId = request.getHeaders().getFirst(USER_ID_HEADER);
            if (userId != null) {
                MDC.put(USER_ID_HEADER, userId);
            }
            
            logRequest(request, correlationId);
            
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();
            
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
            
            return chain.filter(mutatedExchange)
                    .doFinally(signalType -> {
                        logResponse(mutatedExchange);
                        MDC.clear();
                    });
                    
        } catch (Exception e) {
            log.error("Error in request logging filter", e);
            MDC.clear();
            return chain.filter(exchange);
        }
    }

    private String getOrCreateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void logRequest(ServerHttpRequest request, String correlationId) {
        log.info("Incoming request: method={}, uri={}, correlationId={}, remoteAddress={}",
                request.getMethod(),
                request.getURI(),
                correlationId,
                request.getRemoteAddress());
        
        if (log.isDebugEnabled()) {
            HttpHeaders headers = request.getHeaders();
            log.debug("Request headers: {}", sanitizeHeaders(headers));
        }
    }

    private void logResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        Long startTime = exchange.getAttribute(REQUEST_START_TIME);
        
        long duration = startTime != null 
                ? System.currentTimeMillis() - startTime 
                : 0;
        
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        log.info("Outgoing response: status={}, correlationId={}, duration={}ms",
                response.getStatusCode(),
                correlationId,
                duration);
        
        if (response.getHeaders().getFirst(CORRELATION_ID_HEADER) == null) {
            response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }
    }

    private String sanitizeHeaders(HttpHeaders headers) {
        HttpHeaders sanitized = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (key.equalsIgnoreCase("authorization") || 
                key.equalsIgnoreCase("cookie") ||
                key.equalsIgnoreCase("set-cookie")) {
                sanitized.add(key, "[REDACTED]");
            } else {
                sanitized.addAll(key, value);
            }
        });
        return sanitized.toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
