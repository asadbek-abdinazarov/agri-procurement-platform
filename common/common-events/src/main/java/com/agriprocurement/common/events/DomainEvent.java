package com.agriprocurement.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.agriprocurement.common.events.procurement.ProcurementCreatedEvent;
import com.agriprocurement.common.events.procurement.BidSubmittedEvent;
import com.agriprocurement.common.events.order.OrderCreatedEvent;
import com.agriprocurement.common.events.inventory.InventoryReservedEvent;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProcurementCreatedEvent.class, name = "PROCUREMENT_CREATED"),
    @JsonSubTypes.Type(value = BidSubmittedEvent.class, name = "BID_SUBMITTED"),
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = InventoryReservedEvent.class, name = "INVENTORY_RESERVED")
})
public abstract class DomainEvent {
    
    private final String eventId;
    private final String eventType;
    private final String aggregateId;
    private final Instant timestamp;
    private final Integer version;
    private EventMetadata metadata;

    protected DomainEvent(String eventType, String aggregateId, Integer version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
        this.version = version;
    }

    protected DomainEvent(String eventId, String eventType, String aggregateId, 
                         Instant timestamp, Integer version, EventMetadata metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.timestamp = timestamp;
        this.version = version;
        this.metadata = metadata;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Integer getVersion() {
        return version;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }
}
