package com.agriprocurement.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID customerId;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SagaStatus sagaStatus;
    
    @Column(length = 500)
    private String failureReason;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        FAILED
    }
    
    public enum SagaStatus {
        STARTED,
        INVENTORY_RESERVED,
        PAYMENT_PROCESSED,
        COMPLETED,
        COMPENSATING,
        COMPENSATED
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void markAsConfirmed() {
        this.status = OrderStatus.CONFIRMED;
        this.sagaStatus = SagaStatus.COMPLETED;
    }
    
    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }
    
    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.sagaStatus = SagaStatus.COMPENSATED;
    }
    
    public void updateSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }
}
