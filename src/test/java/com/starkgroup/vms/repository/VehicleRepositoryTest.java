package com.starkgroup.vms.repository;

import com.starkgroup.vms.config.JpaAuditingConfig;
import com.starkgroup.vms.model.FuelType;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
import com.starkgroup.vms.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("local")
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();

        sampleVehicle = Vehicle.builder()
                .vin("1HGCR2F83HA000000")
                .licensePlate("ABC-1234")
                .make("Honda")
                .model("Accord")
                .year(2023)
                .type(VehicleType.SEDAN)
                .status(VehicleStatus.AVAILABLE)
                .fuelType(FuelType.PETROL)
                .mileage(15000.0)
                .fuelCapacity(60.0)
                .currentFuelLevelPercentage(85)
                .color("Silver")
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should persist and retrieve vehicle by VIN")
    void testSaveAndFindByVin() {
        vehicleRepository.save(sampleVehicle);

        Optional<Vehicle> found = vehicleRepository.findByVinIgnoreCaseAndDeletedFalse("1HGCR2F83HA000000");

        assertThat(found).isPresent();
        assertThat(found.get().getLicensePlate()).isEqualTo("ABC-1234");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should detect existing VIN ignoring case and deleted status")
    void testExistsByVin() {
        vehicleRepository.save(sampleVehicle);

        boolean exists = vehicleRepository.existsByVinIgnoreCaseAndDeletedFalse("1hgcr2f83ha000000");
        assertThat(exists).isTrue();

        boolean notExists = vehicleRepository.existsByVinIgnoreCaseAndDeletedFalse("NONEXISTENTVIN123");
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should calculate average mileage correctly")
    void testAverageMileage() {
        vehicleRepository.save(sampleVehicle);

        Vehicle secondVehicle = Vehicle.builder()
                .vin("1HGCR2F83HA000001")
                .licensePlate("XYZ-9876")
                .make("Toyota")
                .model("Camry")
                .year(2022)
                .type(VehicleType.SEDAN)
                .status(VehicleStatus.IN_USE)
                .fuelType(FuelType.HYBRID)
                .mileage(25000.0)
                .deleted(false)
                .build();
        vehicleRepository.save(secondVehicle);

        Double avgMileage = vehicleRepository.getAverageMileage();
        assertThat(avgMileage).isEqualTo(20000.0);
    }
}

