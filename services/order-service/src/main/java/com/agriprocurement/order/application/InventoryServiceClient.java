package com.agriprocurement.order.application;

import com.agriprocurement.order.application.dto.InventoryReservationRequest;
import com.agriprocurement.order.application.dto.InventoryReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "inventory-service", path = "/api/v1/inventory")
public interface InventoryServiceClient {
    
    @PostMapping("/reservations")
    InventoryReservationResponse reserveInventory(@RequestBody InventoryReservationRequest request);
    
    @DeleteMapping("/reservations/{reservationId}")
    void releaseReservation(@PathVariable("reservationId") UUID reservationId);
}
