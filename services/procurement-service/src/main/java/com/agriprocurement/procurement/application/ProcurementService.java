package com.agriprocurement.procurement.application;

import com.agriprocurement.common.domain.exception.EntityNotFoundException;
import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.procurement.BidSubmittedEvent;
import com.agriprocurement.common.events.procurement.ProcurementCreatedEvent;
import com.agriprocurement.common.events.publisher.KafkaEventPublisher;
import com.agriprocurement.procurement.domain.Bid;
import com.agriprocurement.procurement.domain.Procurement;
import com.agriprocurement.procurement.domain.ProcurementRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

    private final ProcurementRepository procurementRepository;
    private final KafkaEventPublisher eventPublisher;
    private final ProcurementCacheService cacheService;

    @Transactional
    @CircuitBreaker(name = "procurementService", fallbackMethod = "createProcurementFallback")
    @Retry(name = "procurementService")
    @Bulkhead(name = "procurementService")
    public ProcurementResponse createProcurement(CreateProcurementRequest request) {
        log.info("Creating procurement: {}", request.title());

        Procurement procurement = new Procurement(
            request.title(),
            request.description(),
            request.getQuantity(),
            request.getBudget(),
            request.deadline(),
            request.buyerId()
        );

        procurement = procurementRepository.save(procurement);
        log.info("Procurement created with ID: {}", procurement.getId());

        // Publish domain event
        DomainEvent event = new ProcurementCreatedEvent(
            procurement.getId(),
            procurement.getTitle(),
            procurement.getDescription(),
            procurement.getQuantity().amount(),
            procurement.getBudget().amount(),
            procurement.getDeadline()
        );
        eventPublisher.publishEvent(event);

        return ProcurementResponse.from(procurement);
    }

    @Transactional
    @CircuitBreaker(name = "procurementService", fallbackMethod = "submitBidFallback")
    @Retry(name = "procurementService")
    @Bulkhead(name = "procurementService")
    @CacheEvict(value = "procurements", key = "#request.procurementId()")
    public ProcurementResponse submitBid(SubmitBidRequest request) {
        log.info("Submitting bid for procurement: {}", request.procurementId());

        Procurement procurement = procurementRepository.findByIdWithBids(request.procurementId())
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + request.procurementId()));

        Bid bid = procurement.addBid(request.vendorId(), request.getBidAmount());
        if (request.notes() != null && !request.notes().isEmpty()) {
            // Note: We'd need to add a setter or constructor parameter for notes in Bid
        }

        procurement = procurementRepository.save(procurement);
        log.info("Bid submitted with ID: {}", bid.getId());

        // Publish domain event
        DomainEvent event = new BidSubmittedEvent(
            procurement.getId(),
            bid.getId(),
            request.vendorId(),
            request.getBidAmount().amount(),
            bid.getBidDate()
        );
        eventPublisher.publishEvent(event);

        return ProcurementResponse.from(procurement);
    }

    @Transactional
    @CircuitBreaker(name = "procurementService")
    @Retry(name = "procurementService")
    @CacheEvict(value = "procurements", key = "#procurementId")
    public ProcurementResponse awardProcurement(String procurementId, String bidId) {
        log.info("Awarding procurement {} to bid {}", procurementId, bidId);

        Procurement procurement = procurementRepository.findByIdWithBids(procurementId)
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + procurementId));

        procurement.awardBid(bidId);
        procurement = procurementRepository.save(procurement);

        log.info("Procurement {} awarded to bid {}", procurementId, bidId);

        return ProcurementResponse.fromWithBids(procurement);
    }

    @Transactional
    @CacheEvict(value = "procurements", key = "#procurementId")
    public void publishProcurement(String procurementId) {
        log.info("Publishing procurement: {}", procurementId);

        Procurement procurement = procurementRepository.findById(procurementId)
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + procurementId));

        procurement.publish();
        procurement.openBidding();
        procurementRepository.save(procurement);

        log.info("Procurement {} published and bidding opened", procurementId);
    }

    @Transactional
    @CacheEvict(value = "procurements", key = "#procurementId")
    public void closeBidding(String procurementId) {
        log.info("Closing bidding for procurement: {}", procurementId);

        Procurement procurement = procurementRepository.findById(procurementId)
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + procurementId));

        procurement.closeBidding();
        procurementRepository.save(procurement);

        log.info("Bidding closed for procurement {}", procurementId);
    }

    @Transactional
    @CacheEvict(value = "procurements", key = "#procurementId")
    public void cancelProcurement(String procurementId) {
        log.info("Cancelling procurement: {}", procurementId);

        Procurement procurement = procurementRepository.findByIdWithBids(procurementId)
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + procurementId));

        procurement.cancel();
        procurementRepository.save(procurement);

        log.info("Procurement {} cancelled", procurementId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "procurements", key = "#procurementId")
    @CircuitBreaker(name = "procurementService")
    public ProcurementResponse getProcurement(String procurementId) {
        log.debug("Fetching procurement: {}", procurementId);

        Procurement procurement = procurementRepository.findByIdWithBids(procurementId)
            .orElseThrow(() -> new EntityNotFoundException("Procurement not found: " + procurementId));

        return ProcurementResponse.fromWithBids(procurement);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "procurementService")
    public List<ProcurementResponse> listProcurements() {
        log.debug("Listing all procurements");
        return procurementRepository.findAll().stream()
            .map(ProcurementResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProcurementResponse> listActiveProcurements() {
        log.debug("Listing active procurements");
        return procurementRepository.findActiveProcurements().stream()
            .map(ProcurementResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProcurementResponse> listProcurementsByBuyer(String buyerId) {
        log.debug("Listing procurements for buyer: {}", buyerId);
        return procurementRepository.findByBuyerId(buyerId).stream()
            .map(ProcurementResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void closeExpiredBidding() {
        log.info("Closing expired bidding");
        List<Procurement> expiredProcurements = procurementRepository
            .findByStatusAndDeadlineBefore(Procurement.ProcurementStatus.BIDDING_OPEN, LocalDateTime.now());

        for (Procurement procurement : expiredProcurements) {
            procurement.closeBidding();
            procurementRepository.save(procurement);
            cacheService.evict(procurement.getId());
        }

        log.info("Closed bidding for {} expired procurements", expiredProcurements.size());
    }

    // Fallback methods
    private ProcurementResponse createProcurementFallback(CreateProcurementRequest request, Exception e) {
        log.error("Failed to create procurement: {}", request.title(), e);
        throw new RuntimeException("Service temporarily unavailable. Please try again later.", e);
    }

    private ProcurementResponse submitBidFallback(SubmitBidRequest request, Exception e) {
        log.error("Failed to submit bid for procurement: {}", request.procurementId(), e);
        throw new RuntimeException("Service temporarily unavailable. Please try again later.", e);
    }
}
