package com.agriprocurement.procurement;

import com.agriprocurement.common.domain.exception.DomainException;
import com.agriprocurement.common.domain.valueobject.Money;
import com.agriprocurement.common.domain.valueobject.Quantity;
import com.agriprocurement.procurement.domain.Bid;
import com.agriprocurement.procurement.domain.Procurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcurementDomainTest {

    @Test
    void shouldCreateProcurementInDraftStatus() {
        // Given
        String title = "Agricultural Equipment Procurement";
        String description = "Procurement for farming equipment";
        Quantity quantity = Quantity.of(100, Quantity.Unit.PIECE);
        Money budget = Money.of(50000, "USD");
        LocalDateTime deadline = LocalDateTime.now().plusDays(30);
        UUID buyerId = UUID.randomUUID();

        // When
        Procurement procurement = new Procurement(title, description, quantity, budget, deadline, buyerId);

        // Then
        assertNotNull(procurement);
        assertEquals(title, procurement.getTitle());
        assertEquals(description, procurement.getDescription());
        assertEquals(quantity, procurement.getQuantity());
        assertEquals(budget, procurement.getBudget());
        assertEquals(deadline, procurement.getDeadline());
        assertEquals(buyerId, procurement.getBuyerId());
        assertEquals(Procurement.ProcurementStatus.DRAFT, procurement.getStatus());
        assertTrue(procurement.getBids().isEmpty());
    }

    @Test
    void shouldPublishDraftProcurement() {
        // Given
        Procurement procurement = createValidProcurement();

        // When
        procurement.publish();

        // Then
        assertEquals(Procurement.ProcurementStatus.PUBLISHED, procurement.getStatus());
    }

    @Test
    void shouldNotPublishNonDraftProcurement() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();

        // When & Then
        assertThrows(DomainException.class, procurement::publish);
    }

    @Test
    void shouldOpenBiddingOnPublishedProcurement() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();

        // When
        procurement.openBidding();

        // Then
        assertEquals(Procurement.ProcurementStatus.BIDDING_OPEN, procurement.getStatus());
    }

    @Test
    void shouldNotOpenBiddingOnDraftProcurement() {
        // Given
        Procurement procurement = createValidProcurement();

        // When & Then
        assertThrows(DomainException.class, procurement::openBidding);
    }

    @Test
    void shouldAddBidWhenBiddingIsOpen() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(45000, "USD");

        // When
        Bid bid = procurement.addBid(vendorId, bidAmount);

        // Then
        assertNotNull(bid);
        assertEquals(vendorId, bid.getVendorId());
        assertEquals(bidAmount, bid.getAmount());
        assertEquals(Bid.BidStatus.SUBMITTED, bid.getStatus());
        assertEquals(1, procurement.getBids().size());
    }

    @Test
    void shouldNotAddBidWhenBiddingIsNotOpen() {
        // Given
        Procurement procurement = createValidProcurement();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(45000, "USD");

        // When & Then
        assertThrows(DomainException.class, () -> procurement.addBid(vendorId, bidAmount));
    }

    @Test
    void shouldNotAddBidExceedingBudget() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(60000, "USD");

        // When & Then
        assertThrows(DomainException.class, () -> procurement.addBid(vendorId, bidAmount));
    }

    @Test
    void shouldNotAllowDuplicateBidsFromSameVendor() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount1 = Money.of(45000, "USD");
        Money bidAmount2 = Money.of(44000, "USD");

        // When
        procurement.addBid(vendorId, bidAmount1);

        // Then
        assertThrows(DomainException.class, () -> procurement.addBid(vendorId, bidAmount2));
    }

    @Test
    void shouldCloseBiddingWhenOpen() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();

        // When
        procurement.closeBidding();

        // Then
        assertEquals(Procurement.ProcurementStatus.BIDDING_CLOSED, procurement.getStatus());
    }

    @Test
    void shouldAwardBidAfterBiddingClosed() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(45000, "USD");
        Bid bid = procurement.addBid(vendorId, bidAmount);
        procurement.closeBidding();

        // When
        procurement.awardBid(bid.getId());

        // Then
        assertEquals(Procurement.ProcurementStatus.AWARDED, procurement.getStatus());
        assertEquals(bid.getId(), procurement.getAwardedBidId());
        assertEquals(Bid.BidStatus.ACCEPTED, bid.getStatus());
    }

    @Test
    void shouldNotAwardBidBeforeBiddingClosed() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(45000, "USD");
        Bid bid = procurement.addBid(vendorId, bidAmount);

        // When & Then
        assertThrows(DomainException.class, () -> procurement.awardBid(bid.getId()));
    }

    @Test
    void shouldRejectOtherBidsWhenAwardingOne() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        
        UUID vendorId1 = UUID.randomUUID();
        UUID vendorId2 = UUID.randomUUID();
        Money bidAmount1 = Money.of(45000, "USD");
        Money bidAmount2 = Money.of(46000, "USD");
        
        Bid bid1 = procurement.addBid(vendorId1, bidAmount1);
        Bid bid2 = procurement.addBid(vendorId2, bidAmount2);
        procurement.closeBidding();

        // When
        procurement.awardBid(bid1.getId());

        // Then
        assertEquals(Bid.BidStatus.ACCEPTED, bid1.getStatus());
        assertEquals(Bid.BidStatus.REJECTED, bid2.getStatus());
    }

    @Test
    void shouldCancelProcurement() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();

        // When
        procurement.cancel();

        // Then
        assertEquals(Procurement.ProcurementStatus.CANCELLED, procurement.getStatus());
    }

    @Test
    void shouldNotCancelAwardedProcurement() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        UUID vendorId = UUID.randomUUID();
        Money bidAmount = Money.of(45000, "USD");
        Bid bid = procurement.addBid(vendorId, bidAmount);
        procurement.closeBidding();
        procurement.awardBid(bid.getId());

        // When & Then
        assertThrows(DomainException.class, procurement::cancel);
    }

    @Test
    void shouldRejectAllBidsWhenCancelling() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        
        UUID vendorId1 = UUID.randomUUID();
        UUID vendorId2 = UUID.randomUUID();
        Money bidAmount1 = Money.of(45000, "USD");
        Money bidAmount2 = Money.of(46000, "USD");
        
        Bid bid1 = procurement.addBid(vendorId1, bidAmount1);
        Bid bid2 = procurement.addBid(vendorId2, bidAmount2);

        // When
        procurement.cancel();

        // Then
        assertEquals(Bid.BidStatus.REJECTED, bid1.getStatus());
        assertEquals(Bid.BidStatus.REJECTED, bid2.getStatus());
    }

    @Test
    void shouldGetLowestBid() {
        // Given
        Procurement procurement = createValidProcurement();
        procurement.publish();
        procurement.openBidding();
        
        UUID vendorId1 = UUID.randomUUID();
        UUID vendorId2 = UUID.randomUUID();
        UUID vendorId3 = UUID.randomUUID();
        Money bidAmount1 = Money.of(45000, "USD");
        Money bidAmount2 = Money.of(42000, "USD");
        Money bidAmount3 = Money.of(48000, "USD");
        
        procurement.addBid(vendorId1, bidAmount1);
        Bid lowestBid = procurement.addBid(vendorId2, bidAmount2);
        procurement.addBid(vendorId3, bidAmount3);

        // When
        Bid result = procurement.getLowestBid();

        // Then
        assertEquals(lowestBid.getId(), result.getId());
        assertEquals(bidAmount2, result.getAmount());
    }

    @Test
    void shouldValidateRequiredFields() {
        // When & Then
        assertThrows(DomainException.class, () -> 
            new Procurement(null, "Description", 
                Quantity.of(100, Quantity.Unit.PIECE),
                Money.of(50000, "USD"),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID())
        );

        assertThrows(DomainException.class, () -> 
            new Procurement("Title", null,
                Quantity.of(100, Quantity.Unit.PIECE),
                Money.of(50000, "USD"),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID())
        );

        assertThrows(DomainException.class, () -> 
            new Procurement("Title", "Description",
                null,
                Money.of(50000, "USD"),
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID())
        );

        assertThrows(DomainException.class, () -> 
            new Procurement("Title", "Description",
                Quantity.of(100, Quantity.Unit.PIECE),
                null,
                LocalDateTime.now().plusDays(30),
                UUID.randomUUID())
        );

        assertThrows(DomainException.class, () -> 
            new Procurement("Title", "Description",
                Quantity.of(100, Quantity.Unit.PIECE),
                Money.of(50000, "USD"),
                null,
                UUID.randomUUID())
        );

        assertThrows(DomainException.class, () -> 
            new Procurement("Title", "Description",
                Quantity.of(100, Quantity.Unit.PIECE),
                Money.of(50000, "USD"),
                LocalDateTime.now().plusDays(30),
                null)
        );
    }

    private Procurement createValidProcurement() {
        return new Procurement(
            "Agricultural Equipment Procurement",
            "Procurement for farming equipment",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
    }
}
