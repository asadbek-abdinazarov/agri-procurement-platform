package com.agriprocurement.procurement.application;

import com.agriprocurement.common.domain.valueobject.Money;
import com.agriprocurement.common.domain.valueobject.Quantity;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateProcurementRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    String title,

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    String description,

    @NotNull(message = "Quantity amount is required")
    BigDecimal quantityAmount,

    @NotNull(message = "Quantity unit is required")
    Quantity.Unit quantityUnit,

    @NotNull(message = "Budget amount is required")
    BigDecimal budgetAmount,

    @NotNull(message = "Budget currency is required")
    String budgetCurrency,

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    LocalDateTime deadline,

    @NotNull(message = "Buyer ID is required")
    String buyerId
) {
    public Quantity getQuantity() {
        return Quantity.of(quantityAmount, quantityUnit);
    }

    public Money getBudget() {
        return Money.of(budgetAmount, budgetCurrency);
    }
}
