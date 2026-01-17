package com.agriprocurement.procurement.api;

import com.agriprocurement.procurement.application.CreateProcurementRequest;
import com.agriprocurement.procurement.application.ProcurementResponse;
import com.agriprocurement.procurement.application.ProcurementService;
import com.agriprocurement.procurement.application.SubmitBidRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurements")
@RequiredArgsConstructor
@Slf4j
public class ProcurementController {

    private final ProcurementService procurementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProcurementResponse>> createProcurement(
            @Valid @RequestBody CreateProcurementRequest request) {
        log.info("REST request to create procurement: {}", request.title());
        
        ProcurementResponse response = procurementService.createProcurement(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Procurement created successfully"));
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<ApiResponse<ProcurementResponse>> submitBid(
            @PathVariable String id,
            @Valid @RequestBody SubmitBidRequest request) {
        log.info("REST request to submit bid for procurement: {}", id);
        
        // Ensure the procurement ID in the path matches the request
        SubmitBidRequest validatedRequest = new SubmitBidRequest(
            id,
            request.vendorId(),
            request.bidAmount(),
            request.bidCurrency(),
            request.notes()
        );
        
        ProcurementResponse response = procurementService.submitBid(validatedRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Bid submitted successfully"));
    }

    @PutMapping("/{id}/award")
    public ResponseEntity<ApiResponse<ProcurementResponse>> awardProcurement(
            @PathVariable String id,
            @RequestParam String bidId) {
        log.info("REST request to award procurement {} to bid {}", id, bidId);
        
        ProcurementResponse response = procurementService.awardProcurement(id, bidId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Procurement awarded successfully"));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> publishProcurement(@PathVariable String id) {
        log.info("REST request to publish procurement: {}", id);
        
        procurementService.publishProcurement(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Procurement published successfully"));
    }

    @PutMapping("/{id}/close-bidding")
    public ResponseEntity<ApiResponse<Void>> closeBidding(@PathVariable String id) {
        log.info("REST request to close bidding for procurement: {}", id);
        
        procurementService.closeBidding(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Bidding closed successfully"));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelProcurement(@PathVariable String id) {
        log.info("REST request to cancel procurement: {}", id);
        
        procurementService.cancelProcurement(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Procurement cancelled successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProcurementResponse>> getProcurement(@PathVariable String id) {
        log.debug("REST request to get procurement: {}", id);
        
        ProcurementResponse response = procurementService.getProcurement(id);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProcurementResponse>>> listProcurements(
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to list procurements - buyerId: {}, activeOnly: {}", buyerId, activeOnly);
        
        List<ProcurementResponse> responses;
        
        if (buyerId != null) {
            responses = procurementService.listProcurementsByBuyer(buyerId);
        } else if (activeOnly) {
            responses = procurementService.listActiveProcurements();
        } else {
            responses = procurementService.listProcurements();
        }
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
