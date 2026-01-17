package com.agriprocurement.order.application;

import com.agriprocurement.order.application.dto.*;
import com.agriprocurement.order.domain.Order;
import com.agriprocurement.order.domain.OrderItem;
import com.agriprocurement.order.domain.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {
    
    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Starting order creation saga for customer: {}", request.getCustomerId());
        
        Order order = buildOrder(request);
        order = orderRepository.save(order);
        
        UUID reservationId = null;
        UUID paymentId = null;
        
        try {
            reservationId = reserveInventory(order);
            order.updateSagaStatus(Order.SagaStatus.INVENTORY_RESERVED);
            orderRepository.save(order);
            
            paymentId = processPayment(order);
            order.updateSagaStatus(Order.SagaStatus.PAYMENT_PROCESSED);
            orderRepository.save(order);
            
            order.markAsConfirmed();
            order = orderRepository.save(order);
            
            log.info("Order saga completed successfully for order: {}", order.getId());
            return mapToResponse(order);
            
        } catch (Exception e) {
            log.error("Order saga failed for order: {}, initiating compensation", order.getId(), e);
            compensate(order, reservationId, paymentId, e.getMessage());
            throw new OrderSagaException("Order creation failed: " + e.getMessage(), e);
        }
    }
    
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventory-service")
    private UUID reserveInventory(Order order) {
        log.info("Reserving inventory for order: {}", order.getId());
        
        List<InventoryReservationRequest.ReservationItem> items = order.getItems().stream()
                .map(item -> InventoryReservationRequest.ReservationItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        
        InventoryReservationRequest request = InventoryReservationRequest.builder()
                .orderId(order.getId())
                .items(items)
                .build();
        
        InventoryReservationResponse response = inventoryServiceClient.reserveInventory(request);
        
        if (!response.isSuccess()) {
            throw new InventoryReservationException("Failed to reserve inventory: " + response.getMessage());
        }
        
        log.info("Inventory reserved successfully for order: {}, reservationId: {}", 
                order.getId(), response.getReservationId());
        return response.getReservationId();
    }
    
    private UUID reserveInventoryFallback(Order order, Exception e) {
        log.error("Inventory service unavailable for order: {}", order.getId(), e);
        throw new InventoryReservationException("Inventory service is currently unavailable", e);
    }
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "processPaymentFallback")
    @Retry(name = "payment-service")
    private UUID processPayment(Order order) {
        log.info("Processing payment for order: {}", order.getId());
        
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .currency("USD")
                .build();
        
        PaymentResponse response = paymentServiceClient.processPayment(request);
        
        if (!response.isSuccess()) {
            throw new PaymentProcessingException("Failed to process payment: " + response.getMessage());
        }
        
        log.info("Payment processed successfully for order: {}, paymentId: {}", 
                order.getId(), response.getPaymentId());
        return response.getPaymentId();
    }
    
    private UUID processPaymentFallback(Order order, Exception e) {
        log.error("Payment service unavailable for order: {}", order.getId(), e);
        throw new PaymentProcessingException("Payment service is currently unavailable", e);
    }
    
    @Transactional
    public void compensate(Order order, UUID reservationId, UUID paymentId, String reason) {
        log.warn("Starting compensation for order: {}, reason: {}", order.getId(), reason);
        
        order.updateSagaStatus(Order.SagaStatus.COMPENSATING);
        orderRepository.save(order);
        
        if (paymentId != null) {
            try {
                log.info("Refunding payment: {}", paymentId);
                paymentServiceClient.refundPayment(paymentId);
            } catch (Exception e) {
                log.error("Failed to refund payment: {}", paymentId, e);
            }
        }
        
        if (reservationId != null) {
            try {
                log.info("Releasing inventory reservation: {}", reservationId);
                inventoryServiceClient.releaseReservation(reservationId);
            } catch (Exception e) {
                log.error("Failed to release reservation: {}", reservationId, e);
            }
        }
        
        order.markAsFailed(reason);
        order.updateSagaStatus(Order.SagaStatus.COMPENSATED);
        orderRepository.save(order);
        
        log.info("Compensation completed for order: {}", order.getId());
    }
    
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getOrder(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private Order buildOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(Order.OrderStatus.PENDING)
                .sagaStatus(Order.SagaStatus.STARTED)
                .totalAmount(BigDecimal.ZERO)
                .build();
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            item.calculateTotalPrice();
            order.addItem(item);
        }
        
        order.calculateTotalAmount();
        return order;
    }
    
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .sagaStatus(order.getSagaStatus())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    public static class OrderSagaException extends RuntimeException {
        public OrderSagaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class InventoryReservationException extends RuntimeException {
        public InventoryReservationException(String message) {
            super(message);
        }
        
        public InventoryReservationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) {
            super(message);
        }
        
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
