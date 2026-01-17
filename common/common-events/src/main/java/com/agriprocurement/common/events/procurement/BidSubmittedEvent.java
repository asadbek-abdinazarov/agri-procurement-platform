package com.agriprocurement.common.events.procurement;

import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public final class BidSubmittedEvent extends DomainEvent {

    private final String procurementId;
    private final String bidId;
    private final String vendorId;
    private final BigDecimal amount;
    private final LocalDateTime bidDate;

    public BidSubmittedEvent(String procurementId, String bidId, String vendorId,
                            BigDecimal amount, LocalDateTime bidDate) {
        super("BID_SUBMITTED", bidId, 1);
        this.procurementId = procurementId;
        this.bidId = bidId;
        this.vendorId = vendorId;
        this.amount = amount;
        this.bidDate = bidDate;
    }

    @JsonCreator
    public BidSubmittedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") Integer version,
            @JsonProperty("metadata") EventMetadata metadata,
            @JsonProperty("procurementId") String procurementId,
            @JsonProperty("bidId") String bidId,
            @JsonProperty("vendorId") String vendorId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("bidDate") LocalDateTime bidDate) {
        super(eventId, eventType, aggregateId, timestamp, version, metadata);
        this.procurementId = procurementId;
        this.bidId = bidId;
        this.vendorId = vendorId;
        this.amount = amount;
        this.bidDate = bidDate;
    }

    public String getProcurementId() {
        return procurementId;
    }

    public String getBidId() {
        return bidId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getBidDate() {
        return bidDate;
    }
}
