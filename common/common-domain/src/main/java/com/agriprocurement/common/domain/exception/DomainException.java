package com.agriprocurement.common.domain.exception;

/**
 * Base exception for all domain-related errors.
 * This exception represents business rule violations or domain invariant failures.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
