package com.agriprocurement.procurement.domain;

import com.agriprocurement.common.domain.AggregateRoot;
import com.agriprocurement.common.domain.BaseEntity;
import com.agriprocurement.common.domain.exception.DomainException;
import com.agriprocurement.common.domain.valueobject.Money;
import com.agriprocurement.common.domain.valueobject.Quantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurements")
@Getter
@NoArgsConstructor
public class Procurement extends BaseEntity implements AggregateRoot {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "quantity_amount", nullable = false)),
        @AttributeOverride(name = "unit", column = @Column(name = "quantity_unit", nullable = false))
    })
    private Quantity quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "budget_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "budget_currency", nullable = false))
    })
    private Money budget;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementStatus status;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @OneToMany(mappedBy = "procurement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bid> bids = new ArrayList<>();

    @Column(name = "awarded_bid_id")
    private String awardedBidId;

    public enum ProcurementStatus {
        DRAFT,
        PUBLISHED,
        BIDDING_OPEN,
        BIDDING_CLOSED,
        AWARDED,
        CANCELLED
    }

    public Procurement(String title, String description, Quantity quantity, 
                      Money budget, LocalDateTime deadline, String buyerId) {
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.budget = budget;
        this.deadline = deadline;
        this.buyerId = buyerId;
        this.status = ProcurementStatus.DRAFT;
        validate();
    }

    public void publish() {
        if (status != ProcurementStatus.DRAFT) {
            throw new DomainException("Only draft procurements can be published");
        }
        if (deadline.isBefore(LocalDateTime.now().plusDays(1))) {
            throw new DomainException("Deadline must be at least 1 day in the future");
        }
        this.status = ProcurementStatus.PUBLISHED;
    }

    public void openBidding() {
        if (status != ProcurementStatus.PUBLISHED) {
            throw new DomainException("Only published procurements can open bidding");
        }
        this.status = ProcurementStatus.BIDDING_OPEN;
    }

    public void closeBidding() {
        if (status != ProcurementStatus.BIDDING_OPEN) {
            throw new DomainException("Only procurements with open bidding can be closed");
        }
        this.status = ProcurementStatus.BIDDING_CLOSED;
    }

    public Bid addBid(String vendorId, Money amount) {
        if (status != ProcurementStatus.BIDDING_OPEN) {
            throw new DomainException("Bidding is not open for this procurement");
        }
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new DomainException("Bidding deadline has passed");
        }
        if (amount.isGreaterThan(budget)) {
            throw new DomainException("Bid amount cannot exceed budget");
        }
        
        // Check if vendor already has a bid
        bids.stream()
            .filter(b -> b.getVendorId().equals(vendorId) && b.getStatus() == Bid.BidStatus.SUBMITTED)
            .findFirst()
            .ifPresent(b -> {
                throw new DomainException("Vendor has already submitted a bid");
            });

        Bid bid = new Bid(this, vendorId, amount);
        bids.add(bid);
        return bid;
    }

    public void awardBid(String bidId) {
        if (status != ProcurementStatus.BIDDING_CLOSED) {
            throw new DomainException("Bidding must be closed before awarding");
        }

        Bid bid = bids.stream()
            .filter(b -> b.getId().equals(bidId))
            .findFirst()
            .orElseThrow(() -> new DomainException("Bid not found"));

        if (bid.getStatus() != Bid.BidStatus.SUBMITTED) {
            throw new DomainException("Only submitted bids can be awarded");
        }

        bid.accept();
        this.awardedBidId = bidId;
        this.status = ProcurementStatus.AWARDED;

        // Reject all other bids
        bids.stream()
            .filter(b -> !b.getId().equals(bidId) && b.getStatus() == Bid.BidStatus.SUBMITTED)
            .forEach(Bid::reject);
    }

    public void cancel() {
        if (status == ProcurementStatus.AWARDED) {
            throw new DomainException("Cannot cancel an awarded procurement");
        }
        this.status = ProcurementStatus.CANCELLED;
        
        // Reject all submitted bids
        bids.stream()
            .filter(b -> b.getStatus() == Bid.BidStatus.SUBMITTED)
            .forEach(Bid::reject);
    }

    public Bid getLowestBid() {
        return bids.stream()
            .filter(b -> b.getStatus() == Bid.BidStatus.SUBMITTED)
            .min((b1, b2) -> {
                if (b1.getAmount().isLessThan(b2.getAmount())) return -1;
                if (b1.getAmount().isGreaterThan(b2.getAmount())) return 1;
                return 0;
            })
            .orElse(null);
    }

    private void validate() {
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException("Title is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new DomainException("Description is required");
        }
        if (quantity == null) {
            throw new DomainException("Quantity is required");
        }
        if (budget == null || budget.isZero()) {
            throw new DomainException("Budget is required and must be greater than zero");
        }
        if (deadline == null) {
            throw new DomainException("Deadline is required");
        }
        if (buyerId == null) {
            throw new DomainException("Buyer ID is required");
        }
    }

    public void updateDetails(String title, String description, Quantity quantity, Money budget, LocalDateTime deadline) {
        if (status != ProcurementStatus.DRAFT) {
            throw new DomainException("Only draft procurements can be updated");
        }
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.budget = budget;
        this.deadline = deadline;
        validate();
    }
}
