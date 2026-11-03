package com.starkgroup.vms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vehicle Entity representing fleet assets.
 */
@Entity
@Table(
    name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicles_vin", columnList = "vin", unique = true),
        @Index(name = "idx_vehicles_license_plate", columnList = "license_plate", unique = true),
        @Index(name = "idx_vehicles_status", columnList = "status"),
        @Index(name = "idx_vehicles_type", columnList = "type"),
        @Index(name = "idx_vehicles_deleted", columnList = "deleted")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vin", nullable = false, length = 17, unique = true)
    private String vin;

    @Column(name = "license_plate", nullable = false, length = 20, unique = true)
    private String licensePlate;

    @Column(name = "make", nullable = false, length = 50)
    private String make;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VehicleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    private FuelType fuelType;

    @Column(name = "mileage", nullable = false)
    private Double mileage;

    @Column(name = "fuel_capacity")
    private Double fuelCapacity;

    @Column(name = "current_fuel_level_percentage")
    private Integer currentFuelLevelPercentage;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "assigned_driver_id", length = 100)
    private String assignedDriverId;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_due_mileage")
    private Double nextServiceDueMileage;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}

