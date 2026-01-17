package com.agriprocurement.order.application;

import com.agriprocurement.order.application.dto.PaymentRequest;
import com.agriprocurement.order.application.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentServiceClient {
    
    @PostMapping
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
    
    @DeleteMapping("/{paymentId}/refund")
    void refundPayment(@PathVariable("paymentId") UUID paymentId);
}
