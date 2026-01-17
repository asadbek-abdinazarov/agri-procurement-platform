package com.agriprocurement.common.events.procurement;

import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public final class ProcurementCreatedEvent extends DomainEvent {

    private final String procurementId;
    private final String title;
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal budget;
    private final LocalDateTime deadline;

    public ProcurementCreatedEvent(String procurementId, String title, String description,
                                   BigDecimal quantity, BigDecimal budget, LocalDateTime deadline) {
        super("PROCUREMENT_CREATED", procurementId, 1);
        this.procurementId = procurementId;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.budget = budget;
        this.deadline = deadline;
    }

    @JsonCreator
    public ProcurementCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") Integer version,
            @JsonProperty("metadata") EventMetadata metadata,
            @JsonProperty("procurementId") String procurementId,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("budget") BigDecimal budget,
            @JsonProperty("deadline") LocalDateTime deadline) {
        super(eventId, eventType, aggregateId, timestamp, version, metadata);
        this.procurementId = procurementId;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.budget = budget;
        this.deadline = deadline;
    }

    public String getProcurementId() {
        return procurementId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
}
