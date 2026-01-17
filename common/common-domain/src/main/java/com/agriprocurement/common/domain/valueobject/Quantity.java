package com.agriprocurement.common.domain.valueobject;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing a quantity with a unit of measure.
 */
public record Quantity(
    @NotNull @Positive BigDecimal amount,
    @NotNull Unit unit
) {
    public enum Unit {
        KG("Kilogram", "kg"),
        TON("Ton", "t"),
        LITER("Liter", "L"),
        PIECE("Piece", "pc");

        private final String name;
        private final String symbol;

        Unit(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public Quantity {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(unit, "Unit cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public static Quantity of(BigDecimal amount, Unit unit) {
        return new Quantity(amount, unit);
    }

    public static Quantity of(double amount, Unit unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public Quantity add(Quantity other) {
        if (!this.unit.equals(other.unit)) {
            throw new IllegalArgumentException("Cannot add quantities with different units");
        }
        return new Quantity(this.amount.add(other.amount), this.unit);
    }

    public Quantity subtract(Quantity other) {
        if (!this.unit.equals(other.unit)) {
            throw new IllegalArgumentException("Cannot subtract quantities with different units");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Result must be positive");
        }
        return new Quantity(result, this.unit);
    }

    public Quantity multiply(BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }
        return new Quantity(this.amount.multiply(multiplier), this.unit);
    }

    public boolean isGreaterThan(Quantity other) {
        if (!this.unit.equals(other.unit)) {
            throw new IllegalArgumentException("Cannot compare quantities with different units");
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Quantity other) {
        if (!this.unit.equals(other.unit)) {
            throw new IllegalArgumentException("Cannot compare quantities with different units");
        }
        return this.amount.compareTo(other.amount) < 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + unit.getSymbol();
    }
}
