package com.agriprocurement.procurement.application;

import com.agriprocurement.procurement.domain.Bid;
import com.agriprocurement.procurement.domain.Procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record ProcurementResponse(
    String id,
    String title,
    String description,
    BigDecimal quantityAmount,
    String quantityUnit,
    BigDecimal budgetAmount,
    String budgetCurrency,
    LocalDateTime deadline,
    String status,
    String buyerId,
    int bidCount,
    String awardedBidId,
    Instant createdAt,
    Instant updatedAt,
    List<BidResponse> bids
) {
    public static ProcurementResponse from(Procurement procurement) {
        return new ProcurementResponse(
            procurement.getId(),
            procurement.getTitle(),
            procurement.getDescription(),
            procurement.getQuantity().amount(),
            procurement.getQuantity().unit().name(),
            procurement.getBudget().amount(),
            procurement.getBudget().currency().getCurrencyCode(),
            procurement.getDeadline(),
            procurement.getStatus().name(),
            procurement.getBuyerId(),
            procurement.getBids().size(),
            procurement.getAwardedBidId(),
            procurement.getCreatedAt(),
            procurement.getUpdatedAt(),
            null
        );
    }

    public static ProcurementResponse fromWithBids(Procurement procurement) {
        return new ProcurementResponse(
            procurement.getId(),
            procurement.getTitle(),
            procurement.getDescription(),
            procurement.getQuantity().amount(),
            procurement.getQuantity().unit().name(),
            procurement.getBudget().amount(),
            procurement.getBudget().currency().getCurrencyCode(),
            procurement.getDeadline(),
            procurement.getStatus().name(),
            procurement.getBuyerId(),
            procurement.getBids().size(),
            procurement.getAwardedBidId(),
            procurement.getCreatedAt(),
            procurement.getUpdatedAt(),
            procurement.getBids().stream().map(BidResponse::from).toList()
        );
    }

    public record BidResponse(
        String id,
        String vendorId,
        BigDecimal amount,
        String currency,
        LocalDateTime bidDate,
        String status,
        String notes
    ) {
        public static BidResponse from(Bid bid) {
            return new BidResponse(
                bid.getId(),
                bid.getVendorId(),
                bid.getAmount().amount(),
                bid.getAmount().currency().getCurrencyCode(),
                bid.getBidDate(),
                bid.getStatus().name(),
                bid.getNotes()
            );
        }
    }
}
