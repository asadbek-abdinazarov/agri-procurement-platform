package com.agriprocurement.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationResponse {
    
    private UUID reservationId;
    private UUID orderId;
    private boolean success;
    private String message;
}
