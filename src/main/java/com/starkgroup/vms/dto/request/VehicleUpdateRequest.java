package com.starkgroup.vms.dto.request;

import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Payload for updating vehicle specifications and operational details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequest {

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

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

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

