package com.agriprocurement.common.events.outbox;

import com.agriprocurement.common.events.DomainEvent;
import com.agriprocurement.common.events.publisher.KafkaEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OutboxProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;
    private static final int RETENTION_DAYS = 7;
    private static final long PUBLISH_TIMEOUT_MS = 10000;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository,
                          KafkaEventPublisher kafkaEventPublisher,
                          ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvent(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                event.getAggregateId(),
                event.getEventType(),
                payload
            );
            outboxEventRepository.save(outboxEvent);
            logger.debug("Saved event to outbox: eventId={}, eventType={}", 
                        event.getEventId(), event.getEventType());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event to outbox: eventId={}, eventType={}", 
                        event.getEventId(), event.getEventType(), e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> unprocessedEvents = outboxEventRepository
            .findUnprocessedEvents(MAX_RETRIES)
            .stream()
            .limit(BATCH_SIZE)
            .toList();

        if (unprocessedEvents.isEmpty()) {
            return;
        }

        logger.debug("Processing {} unprocessed outbox events", unprocessedEvents.size());

        for (OutboxEvent outboxEvent : unprocessedEvents) {
            try {
                DomainEvent domainEvent = objectMapper.readValue(
                    outboxEvent.getPayload(), 
                    DomainEvent.class
                );

                kafkaEventPublisher.publishEvent(domainEvent)
                    .get(PUBLISH_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                outboxEvent.markAsProcessed();
                outboxEventRepository.save(outboxEvent);
                
                logger.info("Successfully processed outbox event: id={}, eventType={}", 
                           outboxEvent.getId(), outboxEvent.getEventType());
            } catch (Exception e) {
                logger.error("Failed to process outbox event: id={}, eventType={}, attempt={}", 
                            outboxEvent.getId(), outboxEvent.getEventType(), 
                            outboxEvent.getRetryCount() + 1, e);
                
                outboxEvent.incrementRetryCount(e.getMessage());
                outboxEventRepository.save(outboxEvent);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupProcessedEvents() {
        Instant cutoffTime = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        List<OutboxEvent> oldEvents = outboxEventRepository.findProcessedEventsBefore(cutoffTime);
        
        if (!oldEvents.isEmpty()) {
            outboxEventRepository.deleteAll(oldEvents);
            logger.info("Cleaned up {} processed outbox events older than {} days", 
                       oldEvents.size(), RETENTION_DAYS);
        }
    }
}
