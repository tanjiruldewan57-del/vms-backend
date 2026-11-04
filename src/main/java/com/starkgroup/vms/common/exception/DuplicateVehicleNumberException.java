package com.starkgroup.vms.common.exception;

/**
 * Thrown when attempting to register or update a vehicle with an already existing vehicle number.
 */
public class DuplicateVehicleNumberException extends DuplicateResourceException {

    private final String vehicleNumber;

    public DuplicateVehicleNumberException(String vehicleNumber) {
        super(String.format("Vehicle with number '%s' already exists in the system.", vehicleNumber));
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }
}

