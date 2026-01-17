package com.agriprocurement.common.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for recording business-specific metrics across the agricultural procurement platform.
 * Provides counters, timers, and gauges for tracking procurement, orders, and inventory operations.
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    // Procurement Metrics
    private final Counter procurementCreatedCounter;
    private final Counter procurementAwardedCounter;
    private final Counter procurementCancelledCounter;
    private final Timer procurementProcessingTimer;
    private final AtomicInteger activeProcurements;

    // Order Metrics
    private final Counter orderCreatedCounter;
    private final Counter orderCompletedCounter;
    private final Counter orderFailedCounter;
    private final Timer orderProcessingTimer;
    private final AtomicInteger activeOrders;

    // Inventory Metrics
    private final Counter inventoryReservedCounter;
    private final Counter inventoryReleasedCounter;
    private final Timer inventoryCheckTimer;
    private final AtomicInteger reservedInventoryItems;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Initialize Procurement Metrics
        this.procurementCreatedCounter = Counter.builder("agri.procurement.created")
            .description("Total number of procurements created")
            .tag("type", "procurement")
            .register(registry);

        this.procurementAwardedCounter = Counter.builder("agri.procurement.awarded")
            .description("Total number of procurements awarded")
            .tag("type", "procurement")
            .register(registry);

        this.procurementCancelledCounter = Counter.builder("agri.procurement.cancelled")
            .description("Total number of procurements cancelled")
            .tag("type", "procurement")
            .register(registry);

        this.procurementProcessingTimer = Timer.builder("agri.procurement.processing.duration")
            .description("Time taken to process procurement requests")
            .tag("type", "procurement")
            .register(registry);

        this.activeProcurements = new AtomicInteger(0);
        Gauge.builder("agri.procurement.active", activeProcurements, AtomicInteger::get)
            .description("Current number of active procurements")
            .tag("type", "procurement")
            .register(registry);

        // Initialize Order Metrics
        this.orderCreatedCounter = Counter.builder("agri.order.created")
            .description("Total number of orders created")
            .tag("type", "order")
            .register(registry);

        this.orderCompletedCounter = Counter.builder("agri.order.completed")
            .description("Total number of orders completed")
            .tag("type", "order")
            .register(registry);

        this.orderFailedCounter = Counter.builder("agri.order.failed")
            .description("Total number of orders failed")
            .tag("type", "order")
            .register(registry);

        this.orderProcessingTimer = Timer.builder("agri.order.processing.duration")
            .description("Time taken to process orders")
            .tag("type", "order")
            .register(registry);

        this.activeOrders = new AtomicInteger(0);
        Gauge.builder("agri.order.active", activeOrders, AtomicInteger::get)
            .description("Current number of active orders")
            .tag("type", "order")
            .register(registry);

        // Initialize Inventory Metrics
        this.inventoryReservedCounter = Counter.builder("agri.inventory.reserved")
            .description("Total number of inventory items reserved")
            .tag("type", "inventory")
            .register(registry);

        this.inventoryReleasedCounter = Counter.builder("agri.inventory.released")
            .description("Total number of inventory items released")
            .tag("type", "inventory")
            .register(registry);

        this.inventoryCheckTimer = Timer.builder("agri.inventory.check.duration")
            .description("Time taken to check inventory availability")
            .tag("type", "inventory")
            .register(registry);

        this.reservedInventoryItems = new AtomicInteger(0);
        Gauge.builder("agri.inventory.reserved.items", reservedInventoryItems, AtomicInteger::get)
            .description("Current number of reserved inventory items")
            .tag("type", "inventory")
            .register(registry);
    }

    // Procurement Metrics Methods

    public void recordProcurementCreated() {
        procurementCreatedCounter.increment();
        activeProcurements.incrementAndGet();
    }

    public void recordProcurementCreated(String category) {
        Counter.builder("agri.procurement.created")
            .description("Total number of procurements created")
            .tag("type", "procurement")
            .tag("category", category)
            .register(registry)
            .increment();
        activeProcurements.incrementAndGet();
    }

    public void recordProcurementAwarded() {
        procurementAwardedCounter.increment();
        activeProcurements.decrementAndGet();
    }

    public void recordProcurementAwarded(String category) {
        Counter.builder("agri.procurement.awarded")
            .description("Total number of procurements awarded")
            .tag("type", "procurement")
            .tag("category", category)
            .register(registry)
            .increment();
        activeProcurements.decrementAndGet();
    }

    public void recordProcurementCancelled() {
        procurementCancelledCounter.increment();
        activeProcurements.decrementAndGet();
    }

    public void recordProcurementCancelled(String reason) {
        Counter.builder("agri.procurement.cancelled")
            .description("Total number of procurements cancelled")
            .tag("type", "procurement")
            .tag("reason", reason)
            .register(registry)
            .increment();
        activeProcurements.decrementAndGet();
    }

    public void recordProcurementProcessingTime(Duration duration) {
        procurementProcessingTimer.record(duration);
    }

    public Timer.Sample startProcurementTimer() {
        return Timer.start(registry);
    }

    public void stopProcurementTimer(Timer.Sample sample) {
        sample.stop(procurementProcessingTimer);
    }

    // Order Metrics Methods

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
        activeOrders.incrementAndGet();
    }

    public void recordOrderCreated(String orderType) {
        Counter.builder("agri.order.created")
            .description("Total number of orders created")
            .tag("type", "order")
            .tag("order_type", orderType)
            .register(registry)
            .increment();
        activeOrders.incrementAndGet();
    }

    public void recordOrderCompleted() {
        orderCompletedCounter.increment();
        activeOrders.decrementAndGet();
    }

    public void recordOrderCompleted(String orderType) {
        Counter.builder("agri.order.completed")
            .description("Total number of orders completed")
            .tag("type", "order")
            .tag("order_type", orderType)
            .register(registry)
            .increment();
        activeOrders.decrementAndGet();
    }

    public void recordOrderFailed() {
        orderFailedCounter.increment();
        activeOrders.decrementAndGet();
    }

    public void recordOrderFailed(String reason) {
        Counter.builder("agri.order.failed")
            .description("Total number of orders failed")
            .tag("type", "order")
            .tag("reason", reason)
            .register(registry)
            .increment();
        activeOrders.decrementAndGet();
    }

    public void recordOrderProcessingTime(Duration duration) {
        orderProcessingTimer.record(duration);
    }

    public Timer.Sample startOrderTimer() {
        return Timer.start(registry);
    }

    public void stopOrderTimer(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
    }

    // Inventory Metrics Methods

    public void recordInventoryReserved() {
        inventoryReservedCounter.increment();
        reservedInventoryItems.incrementAndGet();
    }

    public void recordInventoryReserved(int quantity) {
        inventoryReservedCounter.increment(quantity);
        reservedInventoryItems.addAndGet(quantity);
    }

    public void recordInventoryReserved(String productType, int quantity) {
        Counter.builder("agri.inventory.reserved")
            .description("Total number of inventory items reserved")
            .tag("type", "inventory")
            .tag("product_type", productType)
            .register(registry)
            .increment(quantity);
        reservedInventoryItems.addAndGet(quantity);
    }

    public void recordInventoryReleased() {
        inventoryReleasedCounter.increment();
        reservedInventoryItems.decrementAndGet();
    }

    public void recordInventoryReleased(int quantity) {
        inventoryReleasedCounter.increment(quantity);
        reservedInventoryItems.addAndGet(-quantity);
    }

    public void recordInventoryReleased(String productType, int quantity) {
        Counter.builder("agri.inventory.released")
            .description("Total number of inventory items released")
            .tag("type", "inventory")
            .tag("product_type", productType)
            .register(registry)
            .increment(quantity);
        reservedInventoryItems.addAndGet(-quantity);
    }

    public void recordInventoryCheckTime(Duration duration) {
        inventoryCheckTimer.record(duration);
    }

    public Timer.Sample startInventoryTimer() {
        return Timer.start(registry);
    }

    public void stopInventoryTimer(Timer.Sample sample) {
        sample.stop(inventoryCheckTimer);
    }

    // Generic metric recording methods

    /**
     * Record a custom counter metric
     */
    public void recordCounter(String name, String... tags) {
        Counter.builder(name)
            .tags(tags)
            .register(registry)
            .increment();
    }

    /**
     * Record a custom timer metric
     */
    public void recordTimer(String name, Duration duration, String... tags) {
        Timer.builder(name)
            .tags(tags)
            .register(registry)
            .record(duration);
    }

    /**
     * Get the meter registry for custom metrics
     */
    public MeterRegistry getRegistry() {
        return registry;
    }
}
