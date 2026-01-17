package com.agriprocurement.common.events.order;

import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderCreatedEvent extends DomainEvent {

    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String customerId, 
                            List<OrderItem> items, BigDecimal totalAmount) {
        super("ORDER_CREATED", orderId, 1);
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") Integer version,
            @JsonProperty("metadata") EventMetadata metadata,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("items") List<OrderItem> items,
            @JsonProperty("totalAmount") BigDecimal totalAmount) {
        super(eventId, eventType, aggregateId, timestamp, version, metadata);
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public record OrderItem(
        @JsonProperty("itemId") String itemId,
        @JsonProperty("productName") String productName,
        @JsonProperty("quantity") BigDecimal quantity,
        @JsonProperty("unitPrice") BigDecimal unitPrice,
        @JsonProperty("totalPrice") BigDecimal totalPrice
    ) {
        @JsonCreator
        public OrderItem {
        }
    }
}
