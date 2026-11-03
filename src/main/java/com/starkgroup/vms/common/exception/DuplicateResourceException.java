package com.starkgroup.vms.common.exception;

/**
 * Thrown when attempting to persist a unique constraint violation (e.g. duplicate VIN, duplicate license plate).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}

