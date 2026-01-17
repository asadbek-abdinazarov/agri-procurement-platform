package com.agriprocurement.common.events.inventory;

import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public final class InventoryReservedEvent extends DomainEvent {

    private final String itemId;
    private final BigDecimal quantity;
    private final String reservationId;
    private final String orderId;

    public InventoryReservedEvent(String itemId, BigDecimal quantity, 
                                 String reservationId, String orderId) {
        super("INVENTORY_RESERVED", reservationId, 1);
        this.itemId = itemId;
        this.quantity = quantity;
        this.reservationId = reservationId;
        this.orderId = orderId;
    }

    @JsonCreator
    public InventoryReservedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") Integer version,
            @JsonProperty("metadata") EventMetadata metadata,
            @JsonProperty("itemId") String itemId,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("reservationId") String reservationId,
            @JsonProperty("orderId") String orderId) {
        super(eventId, eventType, aggregateId, timestamp, version, metadata);
        this.itemId = itemId;
        this.quantity = quantity;
        this.reservationId = reservationId;
        this.orderId = orderId;
    }

    public String getItemId() {
        return itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getOrderId() {
        return orderId;
    }
}
