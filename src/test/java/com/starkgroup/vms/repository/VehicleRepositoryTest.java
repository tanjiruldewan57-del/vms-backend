package com.starkgroup.vms.repository;

import com.starkgroup.vms.config.JpaAuditingConfig;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleStatus;
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

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber("VMS-001")
                .company("Stark Group")
                .type("Truck")
                .brand("Scania")
                .model("R500")
                .status(VehicleStatus.AVAILABLE)
                .build();

        vehicleRepository.save(vehicle);
    }

    @Test
    @DisplayName("existsByVehicleNumberIgnoreCase: Should return true when vehicle number matches regardless of case")
    void testExistsByVehicleNumberIgnoreCase_MatchesCaseInsensitively() {
        assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-001")).isTrue();
        assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("vms-001")).isTrue();
        assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("VMS-999")).isFalse();
    }

    @Test
    @DisplayName("findByVehicleNumberIgnoreCase: Should find vehicle entity with generated primary key and audit timestamps")
    void testFindByVehicleNumberIgnoreCase_ReturnsEntity() {
        Optional<Vehicle> found = vehicleRepository.findByVehicleNumberIgnoreCase("vms-001");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCompany()).isEqualTo("Stark Group");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }
}
