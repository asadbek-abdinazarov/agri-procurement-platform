package com.agriprocurement.order.application.dto;

import com.agriprocurement.order.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private UUID id;
    private UUID customerId;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private Order.SagaStatus sagaStatus;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
