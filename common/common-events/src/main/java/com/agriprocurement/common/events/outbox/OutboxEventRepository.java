package com.agriprocurement.common.events.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents(int maxRetries);

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :cutoffTime")
    List<OutboxEvent> findProcessedEventsBefore(Instant cutoffTime);
}
