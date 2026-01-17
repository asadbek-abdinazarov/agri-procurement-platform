package com.agriprocurement.procurement.application;

import com.agriprocurement.common.domain.valueobject.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubmitBidRequest(
    @NotNull(message = "Procurement ID is required")
    String procurementId,

    @NotNull(message = "Vendor ID is required")
    String vendorId,

    @NotNull(message = "Bid amount is required")
    BigDecimal bidAmount,

    @NotNull(message = "Bid currency is required")
    String bidCurrency,

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    String notes
) {
    public Money getBidAmount() {
        return Money.of(bidAmount, bidCurrency);
    }
}
