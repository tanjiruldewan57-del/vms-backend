package com.starkgroup.vms.common.exception;

/**
 * Thrown when an operation violates fleet management domain invariant rules
 * (e.g., decreasing odometer mileage, invalid status state transitions).
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}

