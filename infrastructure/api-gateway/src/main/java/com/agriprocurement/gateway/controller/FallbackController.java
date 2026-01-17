package com.agriprocurement.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/procurement")
    public ResponseEntity<Map<String, Object>> procurementFallback() {
        log.warn("Procurement service fallback triggered");
        return createFallbackResponse("Procurement Service");
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        log.warn("Order service fallback triggered");
        return createFallbackResponse("Order Service");
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        log.warn("Inventory service fallback triggered");
        return createFallbackResponse("Inventory Service");
    }

    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        log.warn("Payment service fallback triggered");
        return createFallbackResponse("Payment Service");
    }

    @GetMapping("/logistics")
    public ResponseEntity<Map<String, Object>> logisticsFallback() {
        log.warn("Logistics service fallback triggered");
        return createFallbackResponse("Logistics Service");
    }

    @GetMapping("/notification")
    public ResponseEntity<Map<String, Object>> notificationFallback() {
        log.warn("Notification service fallback triggered");
        return createFallbackResponse("Notification Service");
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        log.warn("User service fallback triggered");
        return createFallbackResponse("User Service");
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analyticsFallback() {
        log.warn("Analytics service fallback triggered");
        return createFallbackResponse("Analytics Service");
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("Default fallback triggered");
        return createFallbackResponse("Service");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", serviceName + " is temporarily unavailable. Please try again later.");
        response.put("service", serviceName);
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
