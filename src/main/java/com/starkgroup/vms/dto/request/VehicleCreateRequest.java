package com.starkgroup.vms.dto.request;

import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Payload for registering a new vehicle into the fleet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest {

    @NotBlank(message = "VIN is required")
    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN must follow ISO 3779 standard (17 alphanumeric chars excluding I, O, Q)")
    private String vin;

    @NotBlank(message = "License plate is required")
    @Size(min = 2, max = 20, message = "License plate length must be between 2 and 20 characters")
    private String licensePlate;

    @NotBlank(message = "Vehicle make is required")
    @Size(max = 50, message = "Vehicle make cannot exceed 50 characters")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 50, message = "Vehicle model cannot exceed 50 characters")
    private String model;

    @NotNull(message = "Manufacture year is required")
    @Min(value = 1900, message = "Year must be 1900 or later")
    @Max(value = 2100, message = "Year cannot exceed 2100")
    private Integer year;

    @NotNull(message = "Vehicle type is required")
    private VehicleType type;

    private VehicleStatus status;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Initial mileage is required")
    @PositiveOrZero(message = "Mileage must be zero or positive")
    private Double mileage;

    @Positive(message = "Fuel capacity must be positive")
    private Double fuelCapacity;

    @Min(value = 0, message = "Fuel level percentage cannot be negative")
    @Max(value = 100, message = "Fuel level percentage cannot exceed 100")
    private Integer currentFuelLevelPercentage;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    @Size(max = 100, message = "Assigned driver ID cannot exceed 100 characters")
    private String assignedDriverId;

    private LocalDate lastServiceDate;

    @PositiveOrZero(message = "Next service mileage must be positive or zero")
    private Double nextServiceDueMileage;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}

