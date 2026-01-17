package com.agriprocurement.common.domain.exception;

/**
 * Exception thrown when optimistic locking fails.
 * This typically occurs when two transactions attempt to update the same entity simultaneously,
 * and one transaction has already committed its changes before the other attempts to commit.
 */
public class ConcurrencyException extends DomainException {

    private final String entityType;
    private final String entityId;

    public ConcurrencyException(String entityType, String entityId) {
        super(String.format("Optimistic locking failed for %s with id '%s'. The entity has been modified by another transaction.", 
            entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public ConcurrencyException(String message) {
        super(message);
        this.entityType = null;
        this.entityId = null;
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
        this.entityType = null;
        this.entityId = null;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
