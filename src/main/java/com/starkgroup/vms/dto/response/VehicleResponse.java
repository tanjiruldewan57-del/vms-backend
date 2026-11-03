package com.starkgroup.vms.dto.response;

import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Standard representation of a vehicle resource returned by the REST API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private UUID id;
    private String vin;
    private String licensePlate;
    private String make;
    private String model;
    private Integer year;
    private VehicleType type;
    private VehicleStatus status;
    private FuelType fuelType;
    private Double mileage;
    private Double fuelCapacity;
    private Integer currentFuelLevelPercentage;
    private String color;
    private String assignedDriverId;
    private LocalDate lastServiceDate;
    private Double nextServiceDueMileage;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vin(vehicle.getVin())
                .licensePlate(vehicle.getLicensePlate())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .type(vehicle.getType())
                .status(vehicle.getStatus())
                .fuelType(vehicle.getFuelType())
                .mileage(vehicle.getMileage())
                .fuelCapacity(vehicle.getFuelCapacity())
                .currentFuelLevelPercentage(vehicle.getCurrentFuelLevelPercentage())
                .color(vehicle.getColor())
                .assignedDriverId(vehicle.getAssignedDriverId())
                .lastServiceDate(vehicle.getLastServiceDate())
                .nextServiceDueMileage(vehicle.getNextServiceDueMileage())
                .notes(vehicle.getNotes())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}

