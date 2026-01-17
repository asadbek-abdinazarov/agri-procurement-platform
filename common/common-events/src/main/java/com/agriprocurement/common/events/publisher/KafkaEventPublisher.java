package com.agriprocurement.common.events.publisher;

import com.agriprocurement.common.events.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, 
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<SendResult<String, String>> publishEvent(String topic, DomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String key = event.getAggregateId();
            String eventId = event.getEventId();
            String eventType = event.getEventType();

            logger.debug("Publishing event to topic {}: eventId={}, eventType={}, aggregateId={}", 
                        topic, eventId, eventType, event.getAggregateId());

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published event to topic {}: eventId={}, eventType={}, partition={}, offset={}", 
                               topic, eventId, eventType,
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish event to topic {}: eventId={}, eventType={}", 
                                topic, eventId, eventType, ex);
                }
            });

            return future;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event: eventId={}, eventType={}", 
                        event.getEventId(), event.getEventType(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<SendResult<String, String>> publishEvent(DomainEvent event) {
        String topic = determineTopicFromEventType(event.getEventType());
        return publishEvent(topic, event);
    }

    private String determineTopicFromEventType(String eventType) {
        return switch (eventType) {
            case "PROCUREMENT_CREATED", "BID_SUBMITTED" -> "procurement-events";
            case "ORDER_CREATED" -> "order-events";
            case "INVENTORY_RESERVED" -> "inventory-events";
            default -> "domain-events";
        };
    }
}
