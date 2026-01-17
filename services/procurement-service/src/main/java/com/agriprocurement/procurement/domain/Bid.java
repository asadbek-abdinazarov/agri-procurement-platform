package com.agriprocurement.procurement.domain;

import com.agriprocurement.common.domain.BaseEntity;
import com.agriprocurement.common.domain.exception.DomainException;
import com.agriprocurement.common.domain.valueobject.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@NoArgsConstructor
public class Bid extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_id", nullable = false)
    private Procurement procurement;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "bid_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "bid_currency", nullable = false))
    })
    private Money amount;

    @Column(name = "bid_date", nullable = false)
    private LocalDateTime bidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(length = 1000)
    private String notes;

    public enum BidStatus {
        SUBMITTED,
        ACCEPTED,
        REJECTED
    }

    public Bid(Procurement procurement, String vendorId, Money amount) {
        this.procurement = procurement;
        this.vendorId = vendorId;
        this.amount = amount;
        this.bidDate = LocalDateTime.now();
        this.status = BidStatus.SUBMITTED;
        validate();
    }

    public Bid(Procurement procurement, String vendorId, Money amount, String notes) {
        this(procurement, vendorId, amount);
        this.notes = notes;
    }

    public void accept() {
        if (status != BidStatus.SUBMITTED) {
            throw new DomainException("Only submitted bids can be accepted");
        }
        this.status = BidStatus.ACCEPTED;
    }

    public void reject() {
        if (status != BidStatus.SUBMITTED) {
            throw new DomainException("Only submitted bids can be rejected");
        }
        this.status = BidStatus.REJECTED;
    }

    private void validate() {
        if (procurement == null) {
            throw new DomainException("Procurement is required");
        }
        if (vendorId == null) {
            throw new DomainException("Vendor ID is required");
        }
        if (amount == null || amount.isZero()) {
            throw new DomainException("Bid amount is required and must be greater than zero");
        }
    }
}
